/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.queue

import com.d3.chainadapter.client.RMQConfig
import com.d3.commons.util.GsonInstance
import com.d3.commons.util.createPrettyFixThreadPool
import com.d3.notifications.NOTIFICATIONS_SERVICE_NAME
import com.d3.notifications.event.BasicEvent
import com.d3.notifications.event.RegistrationNotifyEvent
import com.d3.notifications.event.TransferEventType
import com.d3.notifications.event.TransferNotifyEvent
import com.d3.notifications.exception.RepeatableException
import com.d3.notifications.service.NotificationService
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.rabbitmq.client.*
import mu.KLogging
import org.springframework.stereotype.Component
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

private const val TRANSFERS_QUEUE_NAME = "transfers"
private const val REGISTRATIONS_QUEUE_NAME = "registrations"

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
    private val consumerTags = ArrayList<String>()
    private val connectionFactory = ConnectionFactory()
    private val connection: Connection
    private val channel: Channel
    private val gson = GsonInstance.get()
    private val started = AtomicBoolean()

    init {
        connectionFactory.host = rmqConfig.host
        connectionFactory.port = rmqConfig.port
        connection = connectionFactory.newConnection(subscriberExecutorService)
        channel = connection.createChannel()
        channel.queueDeclare(TRANSFERS_QUEUE_NAME, true, false, false, null)
        channel.queueDeclare(REGISTRATIONS_QUEUE_NAME, true, false, false, null)
    }

    /**
     * Listens to events from RabbitMQ
     */
    fun listen() {
        if (!started.compareAndSet(false, true)) {
            throw IllegalStateException("The queue listening process has been already started")
        }
        val transferCallback = { _: String, delivery: Delivery ->
            try {
                val json = String(delivery.body)
                val transferNotifyEvent = gson.fromJson(json, TransferNotifyEvent::class.java)
                logger.info("Got transfer event from queue $TRANSFERS_QUEUE_NAME: $transferNotifyEvent")
                handleTransfer(transferNotifyEvent)
            } catch (e: Exception) {
                logger.error("Cannot handle delivery from queue $TRANSFERS_QUEUE_NAME", e)
            }

        }
        val registrationCallback = { _: String, delivery: Delivery ->
            try {
                val json = String(delivery.body)
                val registrationNotifyEvent = gson.fromJson(json, RegistrationNotifyEvent::class.java)
                logger.info("Got registration event from queue $REGISTRATIONS_QUEUE_NAME: $registrationNotifyEvent")
                handleRegistration(registrationNotifyEvent)
            } catch (e: Exception) {
                logger.error("Cannot handle delivery from queue $REGISTRATIONS_QUEUE_NAME", e)
            }
        }
        consumerTags.add(channel.basicConsume(TRANSFERS_QUEUE_NAME, true, transferCallback, { _ -> }))
        consumerTags.add(channel.basicConsume(REGISTRATIONS_QUEUE_NAME, true, registrationCallback, { _ -> }))
        logger.info("Start listening to events")
    }

    /**
     * Puts transfer event into RabbitMQ
     * @param transferNotifyEvent - event to put
     */
    fun enqueue(transferNotifyEvent: TransferNotifyEvent) {
        enqueue(transferNotifyEvent, TRANSFERS_QUEUE_NAME)
    }

    /**
     * Puts registration event into RabbitMQ
     * @param registrationNotifyEvent - event to put
     */
    fun enqueue(registrationNotifyEvent: RegistrationNotifyEvent) {
        enqueue(registrationNotifyEvent, REGISTRATIONS_QUEUE_NAME)
    }

    /**
     * Puts event into RabbitMQ
     * @param event - event to put
     * @param queue - name of queue to put event into
     */
    private fun enqueue(event: BasicEvent, queue: String) {
        try {
            logger.info("Enqueue event $event to queue $queue")
            val json = gson.toJson(event)
            channel.basicPublish(
                "",
                queue,
                MessageProperties.MINIMAL_PERSISTENT_BASIC,
                json.toByteArray()
            )
            logger.info("Event $event has been successfully published to queue $queue")
        } catch (e: Exception) {
            logger.error("Cannot enqueue $event", e)
        }
    }

    override fun close() {
        consumerTags.forEach {
            tryClose { channel.basicCancel(it) }
        }
        tryClose { channel.close() }
        tryClose { connection.close() }
        tryClose { subscriberExecutorService.shutdownNow() }
    }

    private fun tryClose(close: () -> Unit) {
        try {
            close()
        } catch (e: Exception) {
            logger.error("Cannot close", e)
        }
    }

    /**
     * Iterates through notification services
     * @param event - event to notify
     * @param queueName - name of queue used for a given event
     * @param iterator - iteration logic
     */
    private fun iterateThroughNotificationServices(
        event: BasicEvent,
        queueName: String,
        iterator: (NotificationService) -> Result<Unit, java.lang.Exception>
    ) {
        notificationServices.forEach {
            iterator(it).failure { ex ->
                if (ex is RepeatableException) {
                    // Re-queue if possible
                    logger.warn("Cannot handle event due to error. Try to re-queue $event", ex)
                    enqueue(event, queueName)
                } else {
                    logger.error("Cannot notify: $event", ex)
                }
            }
        }
    }

    /**
     * Handles registration events from RabbitMQ
     * @param registrationNotifyEvent - event to handle
     */
    private fun handleRegistration(registrationNotifyEvent: RegistrationNotifyEvent) {
        iterateThroughNotificationServices(registrationNotifyEvent, REGISTRATIONS_QUEUE_NAME)
        { it.notifyRegistration(registrationNotifyEvent) }
    }

    /**
     * Handles transfer from RabbitMQ
     * @param transferNotifyEvent - event to handle
     */
    private fun handleTransfer(transferNotifyEvent: TransferNotifyEvent) {
        when (transferNotifyEvent.type) {
            TransferEventType.DEPOSIT -> {
                iterateThroughNotificationServices(transferNotifyEvent, TRANSFERS_QUEUE_NAME)
                { it.notifyDeposit(transferNotifyEvent) }
            }
            TransferEventType.ROLLBACK -> {
                iterateThroughNotificationServices(transferNotifyEvent, TRANSFERS_QUEUE_NAME)
                { it.notifyRollback(transferNotifyEvent) }
            }
            TransferEventType.TRANSFER_RECEIVE -> {
                iterateThroughNotificationServices(transferNotifyEvent, TRANSFERS_QUEUE_NAME)
                { it.notifyReceiveFromClient(transferNotifyEvent) }
            }
            TransferEventType.TRANSFER_SEND -> {
                iterateThroughNotificationServices(transferNotifyEvent, TRANSFERS_QUEUE_NAME)
                { it.notifySendToClient(transferNotifyEvent) }
            }
            TransferEventType.WITHDRAWAL -> {
                iterateThroughNotificationServices(transferNotifyEvent, TRANSFERS_QUEUE_NAME)
                { it.notifyWithdrawal(transferNotifyEvent) }
            }
            else -> {
                //TODO normally, this branch won't be called. just for good measure.
                logger.warn("No handler to handle transfer event type ${transferNotifyEvent.type}")
            }
        }
    }

    companion object : KLogging()
}
