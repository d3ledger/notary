/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.service

import com.d3.chainadapter.client.RMQConfig
import com.d3.commons.util.GsonInstance
import com.d3.commons.util.createPrettyFixThreadPool
import com.d3.notifications.NOTIFICATIONS_SERVICE_NAME
import com.d3.notifications.event.*
import com.github.kittinunf.result.Result
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.rabbitmq.client.impl.DefaultExceptionHandler
import mu.KLogging
import java.io.Closeable
import kotlin.system.exitProcess

const val ETH_ASSET_ID = "ether#ethereum"
const val SORA_EVENTS_EXCHANGE_NAME = "sora_notification_events_exchange"
private const val EVENT_TYPE_HEADER = "event_type"
private const val DEDUPLICATION_HEADER = "x-deduplication-header"

/**
 * Notification service used by Sora
 */
class SoraNotificationService(rmqConfig: RMQConfig) : NotificationService, EthSpecificNotificationService,
    Closeable {

    private val subscriberExecutorService = createPrettyFixThreadPool(NOTIFICATIONS_SERVICE_NAME, "sora_events_queue")
    private val connectionFactory = ConnectionFactory()

    init {
        connectionFactory.host = rmqConfig.host
        connectionFactory.port = rmqConfig.port
        connectionFactory.exceptionHandler = object : DefaultExceptionHandler() {
            override fun handleConnectionRecoveryException(conn: Connection, exception: Throwable) {
                logger.error("RMQ connection error", exception)
                exitProcess(1)
            }

            override fun handleUnexpectedConnectionDriverException(
                conn: Connection,
                exception: Throwable
            ) {
                logger.error("RMQ connection error", exception)
                exitProcess(1)
            }
        }
        connectionFactory.newConnection(subscriberExecutorService).use { connection ->
            connection.createChannel().use { channel ->
                channel.exchangeDeclare(SORA_EVENTS_EXCHANGE_NAME, "fanout", true)
            }
        }
    }

    override fun notifyEthWithdrawalProofs(ethWithdrawalProofsEvent: EthWithdrawalProofsEvent): Result<Unit, Exception> {
        logger.info("Notify enough withdrawal proofs $ethWithdrawalProofsEvent")
        return Result.of {
            postSoraEvent(SoraEthWithdrawalProofsEvent.map(ethWithdrawalProofsEvent))
        }
    }

    override fun notifyFailedRegistration(failedRegistrationNotifyEvent: FailedRegistrationNotifyEvent): Result<Unit, Exception> {
        if (failedRegistrationNotifyEvent.subsystem != RegistrationEventSubsystem.ETH) {
            return Result.of { logger.warn("Sora notification service is not interested in ${failedRegistrationNotifyEvent.subsystem.name} registrations") }
        }
        logger.info("Notify failed Sora registration $failedRegistrationNotifyEvent")
        return Result.of {
            postSoraEvent(SoraFailedRegistrationEvent.map(failedRegistrationNotifyEvent))
        }
    }

    override fun notifyDeposit(transferNotifyEvent: DepositTransferEvent): Result<Unit, Exception> {
        if (transferNotifyEvent.assetName != ETH_ASSET_ID) {
            return Result.of { logger.warn("Sora notification service is not interested in ${transferNotifyEvent.assetName} deposits") }
        }
        logger.info("Notify Sora deposit $transferNotifyEvent")
        return Result.of {
            postSoraEvent(SoraDepositEvent.map(transferNotifyEvent))
        }
    }

    override fun notifySendToClient(transferNotifyEvent: Client2ClientSendTransferEvent): Result<Unit, Exception> {
        logger.info("Notify Sora transfer send $transferNotifyEvent")
        return Result.of {
            logger.warn("'Transfer send' notifications are not supported in Sora")
        }
    }

    override fun notifyReceiveFromClient(transferNotifyEvent: Client2ClientReceiveTransferEvent): Result<Unit, Exception> {
        logger.info("Notify Sora transfer receive $transferNotifyEvent")
        return Result.of {
            logger.warn("'Transfer receive' notifications are not supported in Sora")
        }
    }

    override fun notifyRegistration(registrationNotifyEvent: RegistrationNotifyEvent): Result<Unit, Exception> {
        if (registrationNotifyEvent.subsystem != RegistrationEventSubsystem.ETH) {
            return Result.of { logger.warn("Sora notification service is not interested in ${registrationNotifyEvent.subsystem.name} registrations") }
        }
        logger.info("Notify Sora registration $registrationNotifyEvent")
        return Result.of {
            postSoraEvent(SoraRegistrationEvent.map(registrationNotifyEvent))
        }
    }

    /**
     * Posts Sora event
     * @param event - Sora event to post
     */
    private fun postSoraEvent(event: SoraEvent) {
        val exchange = SORA_EVENTS_EXCHANGE_NAME
        val messageProperties = MessageProperties.MINIMAL_PERSISTENT_BASIC.builder()
            .headers(
                mapOf(
                    Pair(EVENT_TYPE_HEADER, event.javaClass.canonicalName),
                    Pair(DEDUPLICATION_HEADER, event.id)
                )
            ).build()
        try {
            logger.info("Enqueue event $event to exchange $exchange.")
            connectionFactory.newConnection().use { connection ->
                connection.createChannel().use { channel ->
                    channel.basicPublish(
                        exchange,
                        "",
                        messageProperties,
                        event.toJson().toByteArray()
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Cannot publish event $event to exchange $exchange", e)
        }
    }

    override fun close() {
        tryClose { subscriberExecutorService.shutdownNow() }
    }

    private fun tryClose(close: () -> Unit) {
        try {
            close()
        } catch (e: Exception) {
            logger.error("Cannot close", e)
        }
    }

    companion object : KLogging()
}
