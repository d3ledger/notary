package withdrawal.btc.init

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import fee.BtcFeeRateService
import handler.btc.NewBtcClientRegistrationHandler
import healthcheck.HealthyService
import helper.network.addPeerConnectionStatusListener
import helper.network.startChainDownload
import io.reactivex.schedulers.Schedulers
import iroha.protocol.Commands
import mu.KLogging
import org.bitcoinj.core.Address
import org.bitcoinj.core.PeerGroup
import org.bitcoinj.utils.BriefLogFormatter
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import provider.btc.address.BtcRegisteredAddressesProvider
import provider.btc.network.BtcNetworkConfigProvider
import sidechain.iroha.BTC_SIGN_COLLECT_DOMAIN
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.util.getSetDetailCommands
import sidechain.iroha.util.getTransferCommands
import withdrawal.btc.handler.NewFeeRateWasSetHandler
import withdrawal.btc.handler.NewSignatureEventHandler
import withdrawal.btc.handler.WithdrawalTransferEventHandler
import withdrawal.btc.listener.BitcoinBlockChainFeeRateListener
import withdrawal.btc.provider.BtcChangeAddressProvider
import java.io.Closeable

/*
    Class that initiates listeners that will be used to handle Bitcoin withdrawal logic
 */
@Component
class BtcWithdrawalInitialization(
    @Autowired private val peerGroup: PeerGroup,
    @Autowired private val wallet: Wallet,
    @Autowired private val btcChangeAddressProvider: BtcChangeAddressProvider,
    @Qualifier("withdrawalIrohaChainListener")
    @Autowired private val irohaChainListener: IrohaChainListener,
    @Autowired private val btcNetworkConfigProvider: BtcNetworkConfigProvider,
    @Autowired private val withdrawalTransferEventHandler: WithdrawalTransferEventHandler,
    @Autowired private val newSignatureEventHandler: NewSignatureEventHandler,
    @Autowired private val newBtcClientRegistrationHandler: NewBtcClientRegistrationHandler,
    @Autowired private val newFeeRateWasSetHandler: NewFeeRateWasSetHandler,
    @Autowired private val btcRegisteredAddressesProvider: BtcRegisteredAddressesProvider,
    @Autowired private val btcFeeRateService: BtcFeeRateService
) : HealthyService(), Closeable {

    fun init(): Result<Unit, Exception> {
        return btcChangeAddressProvider.getChangeAddress().map { changeAddress ->
            wallet.addWatchedAddress(
                Address.fromBase58(btcNetworkConfigProvider.getConfig(), changeAddress.address)
            )
        }.flatMap {
            btcRegisteredAddressesProvider.getRegisteredAddresses()
        }.map { registeredAddresses ->
            // Adding previously registered addresses to the wallet
            registeredAddresses.map { btcAddress ->
                Address.fromBase58(
                    btcNetworkConfigProvider.getConfig(),
                    btcAddress.address
                )
            }.forEach { address ->
                wallet.addWatchedAddress(address)
            }
            logger.info { "Previously registered addresses were added to the wallet" }
        }.map {
            // Add fee rate listener
            peerGroup.addBlocksDownloadedEventListener(BitcoinBlockChainFeeRateListener(btcFeeRateService))
        }.flatMap {
            initBtcBlockChain()
        }.flatMap { peerGroup ->
            initWithdrawalTransferListener(
                wallet,
                irohaChainListener,
                peerGroup
            )
        }
    }

    /**
     * Initiates listener that listens to withdrawal events in Iroha
     * @param irohaChainListener - listener of Iroha blockchain
     * @return result of initiation process
     */
    private fun initWithdrawalTransferListener(
        wallet: Wallet,
        irohaChainListener: IrohaChainListener,
        peerGroup: PeerGroup
    ): Result<Unit, Exception> {
        return irohaChainListener.getIrohaBlockObservable().map { irohaObservable ->
            irohaObservable.subscribeOn(Schedulers.single())
                .subscribe({ block ->
                    // Handle transfer commands
                    getTransferCommands(block).forEach { command ->
                        withdrawalTransferEventHandler.handleTransferCommand(
                            wallet,
                            command.transferAsset,
                            block.blockV1.payload.createdTime
                        )
                    }
                    // Handle signature appearance commands
                    getSetDetailCommands(block).filter { command -> isNewWithdrawalSignature(command) }
                        .forEach { command ->
                            newSignatureEventHandler.handleNewSignatureCommand(
                                command.setAccountDetail,
                                peerGroup
                            )
                        }
                    // Handle 'set new fee rate' events
                    getSetDetailCommands(block).forEach { command ->
                        newFeeRateWasSetHandler.handleNewFeeRate(command)
                    }
                    // Handle newly registered Bitcoin addresses. We need it to update wallet object.
                    getSetDetailCommands(block).forEach { command ->
                        newBtcClientRegistrationHandler.handleNewClientCommand(command, wallet)
                    }
                }, { ex ->
                    notHealthy()
                    logger.error("Error on transfer events subscription", ex)
                })
            logger.info { "Iroha transfer events listener was initialized" }
        }
    }

    /**
     * Starts Bitcoin block chain download process
     */
    private fun initBtcBlockChain(): Result<PeerGroup, Exception> {
        //Enables short log format for Bitcoin events
        BriefLogFormatter.init()
        return Result.of {
            startChainDownload(peerGroup)
            addPeerConnectionStatusListener(peerGroup, ::notHealthy, ::cured)
            peerGroup
        }
    }

    private fun isNewWithdrawalSignature(command: Commands.Command) =
        command.setAccountDetail.accountId.endsWith("@$BTC_SIGN_COLLECT_DOMAIN")

    override fun close() {
        logger.info { "Closing Bitcoin withdrawal service" }
        peerGroup.stop()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
