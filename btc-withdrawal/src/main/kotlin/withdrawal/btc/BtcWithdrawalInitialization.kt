package withdrawal.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import healthcheck.HealthyService
import helper.network.addPeerConnectionStatusListener
import helper.network.getPeerGroup
import helper.network.startChainDownload
import io.reactivex.schedulers.Schedulers
import iroha.protocol.Commands
import mu.KLogging
import org.bitcoinj.core.PeerGroup
import org.bitcoinj.utils.BriefLogFormatter
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import provider.btc.network.BtcNetworkConfigProvider
import sidechain.iroha.BTC_SIGN_COLLECT_DOMAIN
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.util.getSetDetailCommands
import sidechain.iroha.util.getTransferCommands
import withdrawal.btc.config.BtcWithdrawalConfig
import withdrawal.btc.handler.NewSignatureEventHandler
import withdrawal.btc.handler.WithdrawalTransferEventHandler
import java.io.File
import java.util.concurrent.Executors

/*
    Class that initiates listeners that will be used to handle Bitcoin withdrawal logic
 */
@Component
class BtcWithdrawalInitialization(
    @Autowired private val btcWithdrawalConfig: BtcWithdrawalConfig,
    @Autowired private val irohaChainListener: IrohaChainListener,
    @Autowired private val btcNetworkConfigProvider: BtcNetworkConfigProvider,
    @Autowired private val withdrawalTransferEventHandler: WithdrawalTransferEventHandler,
    @Autowired private val newSignatureEventHandler: NewSignatureEventHandler
) : HealthyService() {

    fun init(): Result<Unit, Exception> {
        val wallet = Wallet.loadFromFile(File(btcWithdrawalConfig.bitcoin.walletPath))
        return initBtcBlockChain(wallet).flatMap { peerGroup ->
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
        return irohaChainListener.getBlockObservable().map { irohaObservable ->
            irohaObservable.subscribeOn(Schedulers.from(Executors.newSingleThreadExecutor()))
                .subscribe({ block ->
                    getTransferCommands(block).forEach { command ->
                        withdrawalTransferEventHandler.handleTransferCommand(
                            wallet,
                            command.transferAsset
                        )
                    }
                    getSetDetailCommands(block).filter { command -> isNewWithdrawalSignature(command) }
                        .forEach { command ->
                            newSignatureEventHandler.handleNewSignatureCommand(
                                command.setAccountDetail,
                                peerGroup
                            )
                        }
                }, { ex ->
                    notHealthy()
                    logger.error("Error on transfer events subscription", ex)
                })
            logger.info { "Iroha transfer events listener was initialized" }
            Unit
        }
    }

    /**
     * Starts Bitcoin block chain download process
     * @param wallet - wallet object that will be enriched with block chain data: sent, unspent transactions, last processed block and etc
     */
    private fun initBtcBlockChain(wallet: Wallet): Result<PeerGroup, Exception> {
        //Enables short log format for Bitcoin events
        BriefLogFormatter.init()
        return Result.of {
            getPeerGroup(
                wallet,
                btcNetworkConfigProvider.getConfig(),
                btcWithdrawalConfig.bitcoin.blockStoragePath
            )
        }.map { peerGroup ->
            startChainDownload(peerGroup, btcWithdrawalConfig.bitcoin.host)
            addPeerConnectionStatusListener(peerGroup, ::notHealthy, ::cured)
            peerGroup
        }
    }

    private fun isNewWithdrawalSignature(command: Commands.Command) =
        command.setAccountDetail.accountId.endsWith("@$BTC_SIGN_COLLECT_DOMAIN")

    /**
     * Logger
     */
    companion object : KLogging()
}
