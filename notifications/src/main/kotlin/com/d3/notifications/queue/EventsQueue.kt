/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.queue

import com.d3.chainadapter.client.RMQConfig
import com.d3.commons.util.GsonInstance
import com.d3.commons.util.createPrettyFixThreadPool
import com.d3.notifications.NOTIFICATIONS_SERVICE_NAME
import com.d3.notifications.exception.RepeatableException
import com.d3.notifications.service.NotificationService
import com.d3.notifications.service.TransferEventType
import com.d3.notifications.service.TransferNotifyEvent
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.rabbitmq.client.*
import mu.KLogging
import org.springframework.stereotype.Component
import java.io.Closeable
import java.lang.IllegalStateException
import java.util.concurrent.atomic.AtomicBoolean

private const val TRANSFERS_QUEUE_NAME = "transfers"

/**
 * Queue of events
 */
@Component
class EventsQueue(
    private val notificationServices: List<NotificationService>,
    rmqConfig: RMQConfig
) : Closeable {

    private val subscriberExecutorService = createPrettyFixThreadPool(
        NOTIFICATIONS_SERVICE_NAME, "events_queue"
    )
    private var consumerTag: String? = null
    private val connectionFactory = ConnectionFactory()
    private val connection: Connection
    private val channel: Channel
    private val gson = GsonInstance.get()
    private val started = AtomicBoolean()

    init {
        connectionFactory.host = rmqConfig.host
        connectionFactory.port = rmqConfig.port
        connection = connectionFactory.newConnection()
        channel = connection.createChannel()
        channel.queueDeclare(TRANSFERS_QUEUE_NAME, true, false, false, null)
    }

    /**
     * Listens to events from RabbitMQ
     */
    fun listen() {
        if (!started.compareAndSet(false, true)) {
            throw IllegalStateException("The queue listening process has been already started")
        }
        val deliverCallback = { _: String, delivery: Delivery ->
            try {
                val json = String(delivery.body)
                val transferNotifyEvent = gson.fromJson(json, TransferNotifyEvent::class.java)
                logger.info("Got event from queue $TRANSFERS_QUEUE_NAME: $transferNotifyEvent")
                handleTransfer(transferNotifyEvent)
            } catch (e: Exception) {
                logger.error("Cannot handle delivery from queue $TRANSFERS_QUEUE_NAME", e)
            }

        }
        consumerTag = channel.basicConsume(TRANSFERS_QUEUE_NAME, true, deliverCallback, { _ -> })
        logger.info("Start listening to events")
    }

    /**
     * Puts event into RabbitMQ
     * @param transferNotifyEvent - event to put
     */
    fun enqueue(transferNotifyEvent: TransferNotifyEvent) {
        try {
            logger.info("Enqueue transfer $transferNotifyEvent")
            val json = gson.toJson(transferNotifyEvent)
            channel.basicPublish(
                "",
                TRANSFERS_QUEUE_NAME,
                MessageProperties.MINIMAL_PERSISTENT_BASIC,
                json.toByteArray()
            )
        } catch (e: Exception) {
            logger.error("Cannot enqueue transfer $transferNotifyEvent", e)
        }
    }

    override fun close() {
        consumerTag?.let {
            channel.basicCancel(it)
        }
        subscriberExecutorService.shutdownNow()
        channel.close()
        connection.close()
    }

    /**
     * Iterates through notification services
     * @param transferNotifyEvent - transfer event to notify
     * @param iterator - iteration logic
     */
    private fun iterateThroughNotificationServices(
        transferNotifyEvent: TransferNotifyEvent,
        iterator: (NotificationService) -> Result<Unit, java.lang.Exception>
    ) {
        notificationServices.forEach {
            iterator(it).failure { ex ->
                if (ex is RepeatableException) {
                    // Re-queue if possible
                    logger.warn("Cannot handle event due to error. Try to re-queue $transferNotifyEvent", ex)
                    enqueue(transferNotifyEvent)
                } else {
                    logger.error("Cannot notify: $transferNotifyEvent", ex)
                }
            }
        }
    }

    /**
     * Handles transfer from RabbitMQ
     * @param transferNotifyEvent - event to handle
     */
    private fun handleTransfer(transferNotifyEvent: TransferNotifyEvent) {
        when (transferNotifyEvent.type) {
            TransferEventType.DEPOSIT -> {
                iterateThroughNotificationServices(transferNotifyEvent) { it.notifyDeposit(transferNotifyEvent) }
            }
            TransferEventType.ROLLBACK -> {
                iterateThroughNotificationServices(transferNotifyEvent) { it.notifyRollback(transferNotifyEvent) }
            }
            TransferEventType.TRANSFER_RECEIVE -> {
                iterateThroughNotificationServices(transferNotifyEvent) { it.notifyReceiveFromClient(transferNotifyEvent) }
            }
            TransferEventType.TRANSFER_SEND -> {
                iterateThroughNotificationServices(transferNotifyEvent) { it.notifySendToClient(transferNotifyEvent) }
            }
            TransferEventType.WITHDRAWAL -> {
                iterateThroughNotificationServices(transferNotifyEvent) { it.notifyWithdrawal(transferNotifyEvent) }
            }
            else -> {
                //TODO normally, this branch won't be called. just for good measure.
                logger.warn("No handler to handle transfer event type ${transferNotifyEvent.type}")
            }
        }
    }

    companion object : KLogging()
}
