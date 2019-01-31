package notifications.service

import com.github.kittinunf.result.Result
import java.math.BigDecimal

/**
 * Notification service interface
 */
interface NotificationService {
    /**
     * Notifies client about deposit event
     * @param transferNotifyEvent - transfer event
     * @return result of operation
     * */
    fun notifyDeposit(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception>

    /**
     * Notifies client about withdrawal event
     * @param transferNotifyEvent - transfer event
     * @return result of operation
     * */
    fun notifyWithdrawal(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception>
}

/**
 * Data class that holds transfer event data
 *
 * @param accountId - account id that will be notified
 * @param amount - transfer amount
 * @param assetName - name of asset
 */
data class TransferNotifyEvent(val accountId: String, val amount: BigDecimal, val assetName: String)
