package com.d3.notifications.service

import com.github.kittinunf.result.Result
import com.d3.notifications.push.WebPushAPIServiceImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Service that is used to notify D3 clients using push notifications
 */
@Component
class PushNotificationService(@Autowired private val webPushAPIService: WebPushAPIServiceImpl) :
    NotificationService {

    override fun notifyWithdrawal(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception> {
        return webPushAPIService.push(
            transferNotifyEvent.accountId,
            "Withdrawal of ${transferNotifyEvent.amount} ${transferNotifyEvent.assetName}"
        )
    }

    override fun notifyDeposit(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception> {
        return webPushAPIService.push(
            transferNotifyEvent.accountId,
            "Deposit of ${transferNotifyEvent.amount} ${transferNotifyEvent.assetName}"
        )
    }
}
