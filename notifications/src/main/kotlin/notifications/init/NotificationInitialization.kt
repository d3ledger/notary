package notifications.init

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import io.reactivex.schedulers.Schedulers
import iroha.protocol.Commands
import mu.KLogging
import notifications.service.NotificationService
import notifications.service.TransferNotifyEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.NOTARY_DOMAIN
import sidechain.iroha.util.getTransferCommands
import java.math.BigDecimal
import java.util.concurrent.Executors

/**
 * Notifications initialization service
 */
@Component
class NotificationInitialization(
    @Autowired private val irohaChainListener: IrohaChainListener,
    @Autowired private val notificationServices: List<NotificationService>
) {

    /**
     * Initiates notification service
     * @param onIrohaChainFailure - function that will be called in case of Iroha failure
     */
    fun init(onIrohaChainFailure: () -> Unit) {
        irohaChainListener.getBlockObservable().map { irohaObservable ->
            irohaObservable
                .subscribeOn(Schedulers.from(Executors.newSingleThreadExecutor()))
                .subscribe({ block ->
                    //Get transfer commands from block
                    getTransferCommands(block).forEach { command ->
                        val transferAsset = command.transferAsset
                        // Notify deposit
                        if (isDeposit(transferAsset)) {
                            handleDepositNotification(transferAsset)
                        }
                        // Notify withdrawal
                        else if (isWithdrawal(transferAsset)) {
                            handleWithdrawalEventNotification(transferAsset)
                        }
                    }
                }, { ex ->
                    logger.error("Error on Iroha subscribe", ex)
                    onIrohaChainFailure()
                })
        }
    }

    // Checks if deposit event
    private fun isDeposit(transferAsset: Commands.TransferAsset): Boolean {
        return transferAsset.srcAccountId.endsWith("@$NOTARY_DOMAIN")
    }

    // Handles deposit event notification
    private fun handleDepositNotification(transferAsset: Commands.TransferAsset) {
        val transferNotifyEvent = TransferNotifyEvent(
            transferAsset.destAccountId,
            BigDecimal(transferAsset.amount),
            transferAsset.assetId
        )
        logger.info { "Notify deposit $transferNotifyEvent" }
        notificationServices.forEach {
            it.notifyDeposit(
                transferNotifyEvent
            ).failure { ex -> logger.error("Cannot notify deposit", ex) }
        }
    }

    // Checks if withdrawal event
    private fun isWithdrawal(transferAsset: Commands.TransferAsset): Boolean {
        return transferAsset.destAccountId.endsWith("@$NOTARY_DOMAIN")
    }

    // Handles withdrawal event notification
    private fun handleWithdrawalEventNotification(transferAsset: Commands.TransferAsset) {
        val transferNotifyEvent = TransferNotifyEvent(
            transferAsset.srcAccountId,
            BigDecimal(transferAsset.amount),
            transferAsset.assetId
        )
        logger.info { "Notify withdrawal $transferNotifyEvent" }
        notificationServices.forEach {
            it.notifyWithdrawal(
                transferNotifyEvent
            ).failure { ex -> logger.error("Cannot notify withdrawal", ex) }
        }
    }

    /**
     *  Initiates notification service.
     *  This overloaded version does nothing on Iroha failure.
     *  Good for testing purposes.
     */
    fun init() {
        init {}
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
