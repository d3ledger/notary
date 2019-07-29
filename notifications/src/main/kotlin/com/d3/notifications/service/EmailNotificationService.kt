/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.service

import com.d3.notifications.provider.D3ClientProvider
import com.d3.notifications.smtp.SMTPService
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import mu.KLogging
import org.springframework.stereotype.Component

const val NOTIFICATION_EMAIL = "no-reply@d3ledger.com"
const val D3_WITHDRAWAL_EMAIL_SUBJECT = "D3 withdrawal"
const val D3_DEPOSIT_EMAIL_SUBJECT = "D3 deposit"
const val D3_DEPOSIT_TRANSFER_SUBJECT = "D3 transfer"
const val D3_DEPOSIT_ROLLBACK_SUBJECT = "D3 rollback"

/**
 * Service for email notifications
 */
@Component
class EmailNotificationService(
    private val smtpService: SMTPService,
    private val d3ClientProvider: D3ClientProvider
) : NotificationService {

    override fun notifySendToClient(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception> {
        val toMessage = if (transferNotifyEvent.to != null) {
            "to ${transferNotifyEvent.to} "
        } else {
            ""
        }
        val message =
            "Dear client, transfer of ${transferNotifyEvent.amount} ${transferNotifyEvent.assetName} from your account ${transferNotifyEvent.accountId} ${toMessage}is successful.\n${transferNotifyEvent.description}"
        return checkClientAndSendMessage(
            transferNotifyEvent.accountId,
            D3_DEPOSIT_TRANSFER_SUBJECT, message
        )
    }

    override fun notifyReceiveFromClient(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception> {
        val fromMessage = if (transferNotifyEvent.from != null) {
            "from ${transferNotifyEvent.from} "
        } else {
            ""
        }
        val message =
            "Dear client, transfer of ${transferNotifyEvent.amount} ${transferNotifyEvent.assetName} ${fromMessage}to your account ${transferNotifyEvent.accountId} is successful.\n${transferNotifyEvent.description}"
        return checkClientAndSendMessage(
            transferNotifyEvent.accountId,
            D3_DEPOSIT_TRANSFER_SUBJECT, message
        )
    }

    override fun notifyRollback(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception> {
        val message =
            "Dear client, unfortunately, we failed to withdraw ${transferNotifyEvent.amount} ${transferNotifyEvent.assetName} from your account ${transferNotifyEvent.accountId}. Rollback has been executed, so your money(including fee) is going back to your account."
        return checkClientAndSendMessage(
            transferNotifyEvent.accountId,
            D3_DEPOSIT_ROLLBACK_SUBJECT, message
        )
    }

    override fun notifyDeposit(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception> {
        val message =
            "Dear client, deposit of ${transferNotifyEvent.amount} ${transferNotifyEvent.assetName} to your account ${transferNotifyEvent.accountId} is successful."
        return checkClientAndSendMessage(
            transferNotifyEvent.accountId,
            D3_DEPOSIT_EMAIL_SUBJECT, message
        )
    }

    override fun notifyWithdrawal(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception> {
        val message =
            "Dear client, withdrawal of ${transferNotifyEvent.amount} ${transferNotifyEvent.assetName} from your account ${transferNotifyEvent.accountId} is successful.\n${transferNotifyEvent.description}"
        return checkClientAndSendMessage(
            transferNotifyEvent.accountId,
            D3_WITHDRAWAL_EMAIL_SUBJECT, message
        )
    }

    /**
     * Checks if client wants to be notified and sends message
     * @param accountId - account id of client
     * @param subject - subject of message
     * @param message - message of notification
     * @return result of operation
     */
    private fun checkClientAndSendMessage(
        accountId: String,
        subject: String,
        message: String
    ): Result<Unit, Exception> {
        return d3ClientProvider.getClient(accountId).flatMap { d3Client ->
            if (!d3Client.enableNotifications) {
                logger.info { "Cannot send push notification. Client ${d3Client.accountId} doesn't want to be notified" }
                return@flatMap Result.of { Unit }
            } else if (d3Client.email == null) {
                logger.warn { "Cannot send push notification. Client ${d3Client.accountId} wants to be notified, but no email was set" }
                return@flatMap Result.of { Unit }
            }
            smtpService.sendMessage(NOTIFICATION_EMAIL, d3Client.email, subject, message)
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
