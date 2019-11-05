/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.service

import com.d3.notifications.config.SoraConfig
import com.d3.notifications.event.*
import com.github.kittinunf.result.Result
import mu.KLogging
import org.json.JSONObject

const val DEPOSIT_URI = "deposit"
const val WITHDRAWAL_URI = "withdrawal"
const val REGISTRATION_URI = "registration"
const val TRANSFER_RECEIVE_URI = "transferReceive"
const val TRANSFER_SEND_URI = "transferSend"

/**
 * Notification service used by Sora
 */
class SoraNotificationService(private val soraConfig: SoraConfig) : NotificationService {

    override fun notifyDeposit(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception> {
        logger.info("Notify Sora deposit $transferNotifyEvent")
        return Result.of {
            postSoraEvent(
                soraConfig.notificationServiceURL + "/" + DEPOSIT_URI,
                SoraDepositEvent.map(transferNotifyEvent)
            )
        }
    }

    override fun notifyWithdrawal(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception> {
        logger.info("Notify Sora withdrawal $transferNotifyEvent")
        return Result.of {
            postSoraEvent(
                soraConfig.notificationServiceURL + "/" + WITHDRAWAL_URI,
                SoraWithdrawalEvent.map(transferNotifyEvent)
            )
        }
    }

    override fun notifySendToClient(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception> {
        logger.info("Notify Sora transfer send $transferNotifyEvent")
        return Result.of {
            postSoraEvent(
                soraConfig.notificationServiceURL + "/" + TRANSFER_SEND_URI,
                SoraTransferEventSend.map(transferNotifyEvent)
            )
        }
    }

    override fun notifyReceiveFromClient(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception> {
        logger.info("Notify Sora transfer receive $transferNotifyEvent")
        return Result.of {
            postSoraEvent(
                soraConfig.notificationServiceURL + "/" + TRANSFER_RECEIVE_URI,
                SoraTransferEventReceive.map(transferNotifyEvent)
            )
        }
    }

    override fun notifyRollback(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception> {
        logger.info("Notify Sora rollback $transferNotifyEvent")
        return Result.of {
            logger.warn("Rollback is not supported in Sora")
        }
    }

    override fun notifyRegistration(registrationNotifyEvent: RegistrationNotifyEvent): Result<Unit, Exception> {
        logger.info("Notify Sora registration $registrationNotifyEvent")
        return Result.of {
            postSoraEvent(
                soraConfig.notificationServiceURL + "/" + REGISTRATION_URI,
                SoraRegistrationEvent.map(registrationNotifyEvent)
            )
        }
    }

    /**
     * Posts Sora event via HTTP
     * @param url - url to post
     * @param event - Sora event to post
     */
    private fun postSoraEvent(url: String, event: SoraEvent) {
        //TODO handle repeatable exceptions
        val response = khttp.post(url = url, json = JSONObject(event))
        if (response.statusCode == 200) {
            logger.info("Sora event $event has been successfully posted")
        } else {
            logger.error(
                "Couldn't post Sora event $event. HTTP status ${response.statusCode}, URL $url, content ${String(
                    response.content
                )}"
            )
        }
    }

    companion object : KLogging()
}
