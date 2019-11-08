/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.service

import com.d3.notifications.event.RegistrationNotifyEvent
import com.d3.notifications.event.TransferNotifyEvent
import com.d3.notifications.provider.D3ClientProvider
import com.d3.notifications.smtp.SMTPService
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import mu.KLogging

const val NOTIFICATION_EMAIL = "no-reply@d3ledger.com"
const val D3_WITHDRAWAL_EMAIL_SUBJECT = "D3 withdrawal"
const val D3_DEPOSIT_EMAIL_SUBJECT = "D3 deposit"
const val D3_DEPOSIT_TRANSFER_SUBJECT = "D3 transfer"
const val D3_ROLLBACK_SUBJECT = "D3 rollback"
const val D3_REGISTRATION_SUBJECT = "D3 registration"

/**
 * Service for email notifications
 */
class EmailNotificationService(
    private val smtpService: SMTPService,
    private val d3ClientProvider: D3ClientProvider
) : NotificationService {

    override fun notifyRegistration(registrationNotifyEvent: RegistrationNotifyEvent): Result<Unit, Exception> {
        val message =
            "Dear client, registration in the ${registrationNotifyEvent.subsystem} subsystem has been successfully completed. Your address is ${registrationNotifyEvent.address}."
        return checkClientAndSendMessage(
            registrationNotifyEvent.accountId,
            D3_REGISTRATION_SUBJECT, message
        )
    }

    override fun notifySendToClient(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception> {
        val toMessage = if (transferNotifyEvent.to != null) {
            "to ${transferNotifyEvent.to} "
        } else {
            ""
        }
        val feeMessage = if (transferNotifyEvent.fee != null) {
            "Fee is ${transferNotifyEvent.fee.amount} ${transferNotifyEvent.fee.assetName}"
        } else {
            ""
        }
        val message =
            "Dear client, transfer of ${transferNotifyEvent.amount} ${transferNotifyEvent.assetName} from your account ${transferNotifyEvent.accountIdToNotify} ${toMessage}is successful. ${transferNotifyEvent.description}\n$feeMessage"
        return checkClientAndSendMessage(
            transferNotifyEvent.accountIdToNotify,
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
            "Dear client, transfer of ${transferNotifyEvent.amount} ${transferNotifyEvent.assetName} ${fromMessage}to your account ${transferNotifyEvent.accountIdToNotify} is successful.\n${transferNotifyEvent.description}"
        return checkClientAndSendMessage(
            transferNotifyEvent.accountIdToNotify,
            D3_DEPOSIT_TRANSFER_SUBJECT, message
        )
    }

    override fun notifyRollback(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception> {
        val feeMessage = if (transferNotifyEvent.fee == null) {
            ""
        } else {
            "Fee ${transferNotifyEvent.fee.amount} ${transferNotifyEvent.fee.assetName} is rolled back as well."
        }
        val message =
            "Dear client, unfortunately, we failed to withdraw ${transferNotifyEvent.amount} ${transferNotifyEvent.assetName} from your account ${transferNotifyEvent.accountIdToNotify}. " +
                    "Rollback has been executed, so your money is going back to your account. $feeMessage\n" +
                    transferNotifyEvent.description
        return checkClientAndSendMessage(
            transferNotifyEvent.accountIdToNotify,
            D3_ROLLBACK_SUBJECT, message
        )
    }

    override fun notifyDeposit(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception> {
        val fromMessage = if (transferNotifyEvent.from != null) {
            "from ${transferNotifyEvent.from} "
        } else {
            ""
        }
        val message =
            "Dear client, deposit of ${transferNotifyEvent.amount} ${transferNotifyEvent.assetName} ${fromMessage}to your account ${transferNotifyEvent.accountIdToNotify} is successful."
        return checkClientAndSendMessage(
            transferNotifyEvent.accountIdToNotify,
            D3_DEPOSIT_EMAIL_SUBJECT, message
        )
    }

    override fun notifyWithdrawal(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception> {
        val toMessage = if (transferNotifyEvent.to != null) {
            "to ${transferNotifyEvent.to} "
        } else {
            ""
        }
        val feeMessage = if (transferNotifyEvent.fee != null) {
            "Fee is ${transferNotifyEvent.fee.amount} ${transferNotifyEvent.fee.assetName}"
        } else {
            ""
        }
        val message =
            "Dear client, withdrawal of ${transferNotifyEvent.amount} ${transferNotifyEvent.assetName} ${toMessage}from your account ${transferNotifyEvent.accountIdToNotify} is successful. ${transferNotifyEvent.description}\n$feeMessage"
        return checkClientAndSendMessage(
            transferNotifyEvent.accountIdToNotify,
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
