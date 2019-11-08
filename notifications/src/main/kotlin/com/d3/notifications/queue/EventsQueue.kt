/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.queue

import com.d3.chainadapter.client.RMQConfig
import com.d3.commons.util.GsonInstance
import com.d3.commons.util.createPrettyFixThreadPool
import com.d3.notifications.NOTIFICATIONS_SERVICE_NAME
import com.d3.notifications.event.*
import com.d3.notifications.exception.RepeatableException
import com.d3.notifications.service.NotificationService
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.rabbitmq.client.*
import mu.KLogging
import org.springframework.stereotype.Component
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

private const val EVENTS_QUEUE_NAME = "notification_events_queue"
private const val EVENT_TYPE_HEADER = "event_type"

/**
 * Queue of events
 */
@Component
class EventsQueue(
    private val notificationServices: List<NotificationService>,
    rmqConfig: RMQConfig
) : Closeable {

    private val autoAck = false
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
        // I think it's enough
        channel.basicQos(16)
        channel.queueDeclare(EVENTS_QUEUE_NAME, true, false, false, null)
    }

    /**
     * Listens to events from RabbitMQ
     */
    fun listen() {
        if (!started.compareAndSet(false, true)) {
            throw IllegalStateException("The queue listening process has been already started")
        }
        val deliveryCallback = { _: String, delivery: Delivery ->
            try {
                val json = String(delivery.body)
                // Headers collection consists of crazy things so we have to convert it to String very carefully
                val eventType = delivery.properties.headers[EVENT_TYPE_HEADER]?.toString() ?: ""
                when (eventType) {
                    // Handle transfers
                    TransferNotifyEvent::class.java.canonicalName -> {
                        val transferNotifyEvent = gson.fromJson(json, TransferNotifyEvent::class.java)
                        logger.info("Got transfer event: $transferNotifyEvent")
                        handleTransfer(transferNotifyEvent)
                    }
                    // Handle registrations
                    RegistrationNotifyEvent::class.java.canonicalName -> {
                        val registrationNotifyEvent = gson.fromJson(json, RegistrationNotifyEvent::class.java)
                        logger.info("Got registration event: $registrationNotifyEvent")
                        handleRegistration(registrationNotifyEvent)
                    }
                    // Handle failed registrations
                    FailedRegistrationNotifyEvent::class.java.canonicalName -> {
                        val failedRegistrationNotifyEvent =
                            gson.fromJson(json, FailedRegistrationNotifyEvent::class.java)
                        logger.info("Got failed registration event: $failedRegistrationNotifyEvent")
                        handleFailedRegistration(failedRegistrationNotifyEvent)
                    }
                    else -> logger.warn("Cannot handle event type $eventType. Message is $json")
                }
            } catch (e: Exception) {
                logger.error("Cannot handle delivery", e)
            } finally {
                if (!autoAck) {
                    channel.basicAck(delivery.envelope.deliveryTag, false)
                }
            }
        }
        consumerTags.add(channel.basicConsume(EVENTS_QUEUE_NAME, autoAck, deliveryCallback, { _ -> }))
        logger.info("Start listening to events")
    }

    /**
     * Puts event into RabbitMQ
     * @param event - event to put
     */
    fun enqueue(event: BasicEvent) {
        val queue = EVENTS_QUEUE_NAME
        val messageProperties = MessageProperties.MINIMAL_PERSISTENT_BASIC.builder()
            .headers(mapOf(Pair(EVENT_TYPE_HEADER, event.javaClass.canonicalName))).build()
        try {
            logger.info("Enqueue event $event to queue $queue")
            val json = gson.toJson(event)
            channel.basicPublish(
                "",
                queue,
                messageProperties,
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
     * @param iterator - iteration logic
     */
    private fun iterateThroughNotificationServices(
        event: BasicEvent,
        iterator: (NotificationService) -> Result<Unit, Exception>
    ) {
        notificationServices.forEach {
            iterator(it).failure { ex ->
                if (ex is RepeatableException) {
                    // Re-queue if possible
                    logger.warn("Cannot handle event due to error. Try to re-queue $event", ex)
                    enqueue(event)
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
        iterateThroughNotificationServices(registrationNotifyEvent)
        { it.notifyRegistration(registrationNotifyEvent) }
    }

    /**
     * Handles failed registration events from RabbitMQ
     * @param failedRegistrationNotifyEvent - event to handle
     */
    private fun handleFailedRegistration(failedRegistrationNotifyEvent: FailedRegistrationNotifyEvent) {
        iterateThroughNotificationServices(failedRegistrationNotifyEvent)
        { it.notifyFailedRegistration(failedRegistrationNotifyEvent) }
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
