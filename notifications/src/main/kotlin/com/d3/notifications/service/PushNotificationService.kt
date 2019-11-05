/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.service

import com.d3.notifications.event.RegistrationNotifyEvent
import com.d3.notifications.event.TransferNotifyEvent
import com.d3.notifications.push.WebPushAPIService
import com.github.kittinunf.result.Result

/**
 * Service that is used to notify D3 clients using push notifications
 */
class PushNotificationService(private val webPushAPIService: WebPushAPIService) :
    NotificationService {

    override fun notifyRegistration(registrationNotifyEvent: RegistrationNotifyEvent): Result<Unit, Exception> {
        return webPushAPIService.push(
            registrationNotifyEvent.accountId,
            "Registration in ${registrationNotifyEvent.subsystem} ${registrationNotifyEvent.address}"
        )
    }

    override fun notifySendToClient(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception> {
        return webPushAPIService.push(
            transferNotifyEvent.accountId,
            "Transfer of ${transferNotifyEvent.amount} ${transferNotifyEvent.assetName}"
        )
    }

    override fun notifyReceiveFromClient(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception> {
        return webPushAPIService.push(
            transferNotifyEvent.accountId,
            "Transfer of ${transferNotifyEvent.amount} ${transferNotifyEvent.assetName}"
        )
    }

    override fun notifyRollback(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception> {
        return webPushAPIService.push(
            transferNotifyEvent.accountId,
            "Rollback of ${transferNotifyEvent.amount} ${transferNotifyEvent.assetName}"
        )
    }

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
