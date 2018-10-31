package withdrawal.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import healthcheck.HealthyService
import io.reactivex.schedulers.Schedulers
import iroha.protocol.Commands
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.util.getTransferCommands
import withdrawal.btc.config.BtcWithdrawalConfig
import java.util.concurrent.Executors

/*
    Class that initiates listeners that will be used to handle Bitcoin withdrawal logic
 */
@Component
class BtcWithdrawalInitialization(
    @Autowired private val btcWithdrawalConfig: BtcWithdrawalConfig,
    @Autowired private val irohaChainListener: IrohaChainListener
) : HealthyService() {

    fun init(): Result<Unit, Exception> {
        return initTransferListener(irohaChainListener)
    }

    /**
     * Initiates listener that listens to withdrawal events in Iroha
     * @param irohaChainListener - listener of Iroha blockchain
     * @return result of initiation process
     */
    private fun initTransferListener(irohaChainListener: IrohaChainListener): Result<Unit, Exception> {
        return irohaChainListener.getBlockObservable().map { irohaObservable ->
            irohaObservable.subscribeOn(Schedulers.from(Executors.newSingleThreadExecutor()))
                .subscribe({ block ->
                    getTransferCommands(block).forEach { command -> handleTransferCommand(command.transferAsset) }
                }, { ex ->
                    notHealthy()
                    logger.error("Error on transfer events subscription", ex)
                })
            logger.info { "Iroha transfer events listener was initialized" }
            Unit
        }
    }

    /**
     * Handles "transfer asset" command
     * @param transferCommand - object with "transfer asset" data: source account, destination account, amount and etc
     */
    private fun handleTransferCommand(transferCommand: Commands.TransferAsset) {
        if (transferCommand.destAccountId != btcWithdrawalConfig.withdrawalCredential.accountId) {
            return
        }
        logger.info {
            "Withdrawal event(" +
                    "from:${transferCommand.srcAccountId} " +
                    "to:${transferCommand.description} " +
                    "amount:${transferCommand.amount})"
        }
        //TODO check if destination address from 'transferCommand.description' is whitelisted
        //TODO create Bitcoin multisignature transaction
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
