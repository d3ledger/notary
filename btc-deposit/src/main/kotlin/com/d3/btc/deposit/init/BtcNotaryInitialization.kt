package com.d3.btc.deposit.init

import com.d3.btc.deposit.config.BtcDepositConfig
import com.d3.btc.deposit.listener.BitcoinBlockChainDepositListener
import com.d3.btc.healthcheck.HealthyService
import com.d3.btc.helper.network.addPeerConnectionStatusListener
import com.d3.btc.helper.network.startChainDownload
import com.d3.btc.listener.NewBtcClientRegistrationListener
import com.d3.btc.provider.BtcRegisteredAddressesProvider
import com.d3.btc.provider.network.BtcNetworkConfigProvider
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import io.reactivex.Observable
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import com.d3.commons.model.IrohaCredential
import mu.KLogging
import com.d3.commons.notary.NotaryImpl
import org.bitcoinj.core.Address
import org.bitcoinj.core.PeerGroup
import org.bitcoinj.utils.BriefLogFormatter
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import com.d3.commons.provider.NotaryPeerListProviderImpl
import com.d3.commons.sidechain.SideChainEvent
import com.d3.commons.sidechain.iroha.IrohaChainListener
import java.io.Closeable
import java.io.File
import java.util.concurrent.Executors

@Component
class BtcNotaryInitialization(
    @Autowired private val peerGroup: PeerGroup,
    @Autowired private val transferWallet: Wallet,
    @Autowired private val btcDepositConfig: BtcDepositConfig,
    @Qualifier("notaryCredential")
    @Autowired private val irohaCredential: IrohaCredential,
    @Autowired private val irohaAPI: IrohaAPI,
    @Autowired private val btcRegisteredAddressesProvider: BtcRegisteredAddressesProvider,
    @Qualifier("depositIrohaChainListener")
    @Autowired private val irohaChainListener: IrohaChainListener,
    @Autowired private val newBtcClientRegistrationListener: NewBtcClientRegistrationListener,
    @Autowired private val btcNetworkConfigProvider: BtcNetworkConfigProvider
) : HealthyService(), Closeable {


    // Executor that will be used to execute Bitcoin deposit listener logic
    private val blockChainDepositListenerExecutor = Executors.newSingleThreadExecutor()

    // Executor that will be used to execute transaction confidence listener logic
    private val confidenceListenerExecutorService = Executors.newSingleThreadExecutor()

    /**
     * Init notary
     */
    fun init(): Result<Unit, Exception> {
        logger.info { "Btc notary initialization" }
        //Enables short log format for Bitcoin events
        BriefLogFormatter.init()
        addPeerConnectionStatusListener(peerGroup, ::notHealthy, ::cured)
        return irohaChainListener.getBlockObservable().map { irohaObservable ->
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
            getBtcEvents(peerGroup, btcDepositConfig.bitcoin.confidenceLevel)
        }.map { btcEvents ->
            val queryAPI = QueryAPI(irohaAPI, irohaCredential.accountId, irohaCredential.keyPair)

            val peerListProvider = NotaryPeerListProviderImpl(
                queryAPI,
                btcDepositConfig.notaryListStorageAccount,
                btcDepositConfig.notaryListSetterAccount
            )
            val notary = NotaryImpl(irohaCredential, irohaAPI, btcEvents, peerListProvider)
            notary.initIrohaConsumer().failure { ex -> throw ex }
        }.map {
            startChainDownload(peerGroup)
        }
    }

    //Checks if address is watched by notary
    fun isWatchedAddress(btcAddress: String) =
        transferWallet.isAddressWatched(Address.fromBase58(btcNetworkConfigProvider.getConfig(), btcAddress))

    /**
     * Returns observable object full of deposit events
     */
    private fun getBtcEvents(
        peerGroup: PeerGroup,
        confidenceLevel: Int
    ): Observable<SideChainEvent.PrimaryBlockChainEvent> {
        return Observable.create<SideChainEvent.PrimaryBlockChainEvent> { emitter ->
            peerGroup.addBlocksDownloadedEventListener(
                blockChainDepositListenerExecutor,
                BitcoinBlockChainDepositListener(
                    btcRegisteredAddressesProvider,
                    emitter,
                    confidenceListenerExecutorService,
                    confidenceLevel
                ) { transferWallet.saveToFile(File(btcDepositConfig.btcTransferWalletPath)) }
            )
        }
    }

    override fun close() {
        logger.info { "Closing Bitcoin notary service" }
        confidenceListenerExecutorService.shutdownNow()
        blockChainDepositListenerExecutor.shutdownNow()
        peerGroup.stop()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
