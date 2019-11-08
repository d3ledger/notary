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

const val ETH_ASSET_ID = "ether#ethereum"

/**
 * Notification service used by Sora
 */
class SoraNotificationService(private val soraConfig: SoraConfig) : NotificationService {

    override fun notifyFailedRegistration(failedRegistrationNotifyEvent: FailedRegistrationNotifyEvent): Result<Unit, Exception> {
        if (failedRegistrationNotifyEvent.subsystem != RegistrationEventSubsystem.ETH) {
            return Result.of { logger.warn("Sora notification service is not interested in ${failedRegistrationNotifyEvent.subsystem.name} registrations") }
        }
        logger.info("Notify failed Sora registration $failedRegistrationNotifyEvent")
        return Result.of {
            postSoraEvent(
                SoraURI.FAILED_REGISTRATION_URI,
                SoraFailedRegistrationEvent.map(failedRegistrationNotifyEvent)
            )
        }
    }

    override fun notifyDeposit(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception> {
        if (transferNotifyEvent.assetName != ETH_ASSET_ID) {
            return Result.of { logger.warn("Sora notification service is not interested in ${transferNotifyEvent.assetName} deposits") }
        }
        logger.info("Notify Sora deposit $transferNotifyEvent")
        return Result.of {
            postSoraEvent(SoraURI.DEPOSIT_URI, SoraDepositEvent.map(transferNotifyEvent))
        }
    }

    override fun notifyWithdrawal(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception> {
        if (transferNotifyEvent.assetName != ETH_ASSET_ID) {
            return Result.of { logger.warn("Sora notification service is not interested in ${transferNotifyEvent.assetName} withdrawals") }
        }
        logger.info("Notify Sora withdrawal $transferNotifyEvent")
        return Result.of {
            postSoraEvent(SoraURI.WITHDRAWAL_URI, SoraWithdrawalEvent.map(transferNotifyEvent))
        }
    }

    override fun notifySendToClient(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception> {
        logger.info("Notify Sora transfer send $transferNotifyEvent")
        return Result.of {
            logger.warn("'Transfer send' notifications are not supported in Sora")
        }
    }

    override fun notifyReceiveFromClient(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception> {
        logger.info("Notify Sora transfer receive $transferNotifyEvent")
        return Result.of {
            logger.warn("'Transfer receive' notifications are not supported in Sora")
        }
    }

    override fun notifyRollback(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception> {
        logger.info("Notify Sora rollback $transferNotifyEvent")
        return Result.of {
            logger.warn("Rollback notifications are not supported in Sora")
        }
    }

    override fun notifyRegistration(registrationNotifyEvent: RegistrationNotifyEvent): Result<Unit, Exception> {
        if (registrationNotifyEvent.subsystem != RegistrationEventSubsystem.ETH) {
            return Result.of { logger.warn("Sora notification service is not interested in ${registrationNotifyEvent.subsystem.name} registrations") }
        }
        logger.info("Notify Sora registration $registrationNotifyEvent")
        return Result.of {
            postSoraEvent(SoraURI.REGISTRATION_URI, SoraRegistrationEvent.map(registrationNotifyEvent))
        }
    }

    /**
     * Posts Sora event via HTTP
     * @param uri - uri of service
     * @param event - Sora event to post
     */
    private fun postSoraEvent(uri: SoraURI, event: SoraEvent) {
        //TODO handle repeatable exceptions
        val url = soraConfig.notificationServiceURL + "/" + uri.uri
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

/**
 * Enumeration of Sora URIs
 */
enum class SoraURI(val uri: String) {
    DEPOSIT_URI("deposit"),
    WITHDRAWAL_URI("withdrawal"),
    REGISTRATION_URI("registration"),
    FAILED_REGISTRATION_URI("failedRegistration")
}
