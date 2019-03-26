package com.d3.btc.deposit.init

import com.d3.btc.deposit.BTC_DEPOSIT_SERVICE_NAME
import com.d3.btc.deposit.config.BtcDepositConfig
import com.d3.btc.deposit.listener.BitcoinBlockChainDepositListener
import com.d3.btc.deposit.service.BtcWalletListenerRestartService
import com.d3.btc.healthcheck.HealthyService
import com.d3.btc.helper.network.addPeerConnectionStatusListener
import com.d3.btc.helper.network.startChainDownload
import com.d3.btc.listener.NewBtcClientRegistrationListener
import com.d3.btc.peer.SharedPeerGroup
import com.d3.btc.provider.BtcRegisteredAddressesProvider
import com.d3.btc.provider.network.BtcNetworkConfigProvider
import com.d3.btc.wallet.checkWalletNetwork
import com.d3.commons.notary.NotaryImpl
import com.d3.commons.sidechain.SideChainEvent
import com.d3.commons.sidechain.iroha.IrohaChainListener
import com.d3.commons.util.createPrettySingleThreadPool
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import io.reactivex.subjects.PublishSubject
import mu.KLogging
import org.bitcoinj.core.Address
import org.bitcoinj.core.PeerGroup
import org.bitcoinj.utils.BriefLogFormatter
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.io.Closeable
import java.io.File
import java.util.concurrent.ExecutorService

@Component
class BtcNotaryInitialization(
    @Autowired private val peerGroup: SharedPeerGroup,
    @Autowired private val transferWallet: Wallet,
    @Autowired private val btcDepositConfig: BtcDepositConfig,
    @Autowired private val notary: NotaryImpl,
    @Autowired private val btcRegisteredAddressesProvider: BtcRegisteredAddressesProvider,
    @Autowired private val btcEventsSource: PublishSubject<SideChainEvent.PrimaryBlockChainEvent>,
    @Qualifier("depositIrohaChainListener")
    @Autowired private val irohaChainListener: IrohaChainListener,
    @Autowired private val newBtcClientRegistrationListener: NewBtcClientRegistrationListener,
    @Autowired private val btcWalletListenerRestartService: BtcWalletListenerRestartService,
    @Qualifier("confidenceListenerExecutorService")
    @Autowired private val confidenceListenerExecutorService: ExecutorService,
    @Autowired private val btcNetworkConfigProvider: BtcNetworkConfigProvider
) : HealthyService(), Closeable {

    // Executor that will be used to execute Bitcoin deposit listener logic
    private val blockChainDepositListenerExecutor =
        createPrettySingleThreadPool(BTC_DEPOSIT_SERVICE_NAME, "blockchain-deposit-listener")

    // Function that is called to save all the transactions in wallet
    private fun onTxSave() {
        transferWallet.saveToFile(File(btcDepositConfig.btcTransferWalletPath))
        logger.info { "Wallet was saved in ${btcDepositConfig.btcTransferWalletPath}" }
    }

    /**
     * Init notary
     */
    fun init(): Result<Unit, Exception> {
        logger.info { "Btc notary initialization" }
        //Enables short log format for Bitcoin events
        BriefLogFormatter.init()
        addPeerConnectionStatusListener(peerGroup, ::notHealthy, ::cured)
        // Check wallet network
        return transferWallet.checkWalletNetwork(btcNetworkConfigProvider.getConfig()).map {
            // Restart wallet listeners
            btcWalletListenerRestartService.restartTransactionListeners(
                transferWallet, ::onTxSave
            )
        }.flatMap {
            irohaChainListener.getBlockObservable()
        }.map { irohaObservable ->
            newBtcClientRegistrationListener.listenToRegisteredClients(
                transferWallet, irohaObservable
            ) {
                // Kill deposit service if Iroha chain listener is not functioning
                close()
            }
            logger.info { "Registration service listener was successfully initialized" }
        }.flatMap {
            btcRegisteredAddressesProvider.getRegisteredAddresses()
        }.map { registeredAddresses ->
            // Adding previously registered addresses to the transferWallet
            registeredAddresses.map { btcAddress ->
                Address.fromBase58(
                    btcNetworkConfigProvider.getConfig(),
                    btcAddress.address
                )
            }.forEach { address ->
                transferWallet.addWatchedAddress(address)
            }
            logger.info { "Previously registered addresses were added to the transferWallet" }
        }.map {
            initBtcEvents(peerGroup, btcDepositConfig.bitcoin.confidenceLevel)
        }.map {
            notary.initIrohaConsumer().failure { ex -> throw ex }
        }.map {
            startChainDownload(peerGroup)
        }
    }

    //Checks if address is watched by notary
    fun isWatchedAddress(btcAddress: String) =
        transferWallet.isAddressWatched(Address.fromBase58(btcNetworkConfigProvider.getConfig(), btcAddress))

    /**
     * Initiates Btc deposit events
     */
    private fun initBtcEvents(
        peerGroup: PeerGroup,
        confidenceLevel: Int
    ) {
        peerGroup.addBlocksDownloadedEventListener(
            blockChainDepositListenerExecutor,
            BitcoinBlockChainDepositListener(
                btcRegisteredAddressesProvider,
                btcEventsSource,
                confidenceListenerExecutorService,
                confidenceLevel,
                ::onTxSave
            )
        )
    }

    override fun close() {
        logger.info { "Closing Bitcoin notary service" }
        blockChainDepositListenerExecutor.shutdownNow()
        peerGroup.stop()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
