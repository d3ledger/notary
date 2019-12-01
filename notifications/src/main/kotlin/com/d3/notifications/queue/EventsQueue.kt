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
import com.d3.notifications.service.EthSpecificNotificationService
import com.d3.notifications.service.NotificationService
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.rabbitmq.client.*
import com.rabbitmq.client.impl.DefaultExceptionHandler
import mu.KLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

private const val EVENTS_QUEUE_NAME = "notification_events_queue"
private const val EVENT_TYPE_HEADER = "event_type"
private const val NOTIFICATION_SERVICE_ID_HEADER = "notification_service_id"
private const val DEDUPLICATION_HEADER = "x-deduplication-header"

/**
 * Queue of events
 */
@Component
class EventsQueue(
    private val notificationServices: List<NotificationService>,
    rmqConfig: RMQConfig
) : Closeable {

    private val autoAck = false
    private val requeue = true
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
        connection = connectionFactory.newConnection(subscriberExecutorService)
        channel = connection.createChannel()
        // I think it's enough
        channel.basicQos(16)
        val arguments = hashMapOf<String, Any>(
            // enable deduplication
            Pair("x-message-deduplication", true),
            // save deduplication data on disk rather that memory
            Pair("x-cache-persistence", "disk"),
            // save deduplication data 1 day
            Pair("x-cache-ttl", 60_000 * 60 * 24)
        )
        channel.queueDeclare(EVENTS_QUEUE_NAME, true, false, false, arguments)
    }

    /**
     * Listens to events from RabbitMQ
     */
    fun listen() {
        if (!started.compareAndSet(false, true)) {
            throw IllegalStateException("The queue listening process has been already started")
        }
        val deliveryCallback = { _: String, delivery: Delivery ->
            val json = String(delivery.body)
            // Headers collection consists of crazy things so we have to convert it to String very carefully
            val eventType = delivery.properties.headers[EVENT_TYPE_HEADER]?.toString() ?: ""
            val serviceId = delivery.properties.headers[NOTIFICATION_SERVICE_ID_HEADER]?.toString() ?: ""

            try {
                logger.info("Got event $json. Event type $eventType with service id $serviceId")
                when (eventType) {
                    // Handle deposit
                    DepositTransferEvent::class.java.canonicalName -> {
                        val transferNotifyEvent = gson.fromJson(json, DepositTransferEvent::class.java)
                        logger.info("Got deposit event: $transferNotifyEvent")
                        handleDepositTransfer(transferNotifyEvent, serviceId)
                    }
                    // Handle client 'send'
                    Client2ClientSendTransferEvent::class.java.canonicalName -> {
                        val transferNotifyEvent = gson.fromJson(json, Client2ClientSendTransferEvent::class.java)
                        logger.info("Got transfer 'send' event: $transferNotifyEvent")
                        handleSendTransfer(transferNotifyEvent, serviceId)
                    }
                    // Handle client 'receive'
                    Client2ClientReceiveTransferEvent::class.java.canonicalName -> {
                        val transferNotifyEvent = gson.fromJson(json, Client2ClientReceiveTransferEvent::class.java)
                        logger.info("Got transfer 'receive' event: $transferNotifyEvent")
                        handleReceiveTransfer(transferNotifyEvent, serviceId)
                    }
                    // Handle registrations
                    RegistrationNotifyEvent::class.java.canonicalName -> {
                        val registrationNotifyEvent = gson.fromJson(json, RegistrationNotifyEvent::class.java)
                        logger.info("Got registration event: $registrationNotifyEvent")
                        handleRegistration(registrationNotifyEvent, serviceId)
                    }
                    // Handle failed registrations
                    FailedRegistrationNotifyEvent::class.java.canonicalName -> {
                        val failedRegistrationNotifyEvent =
                            gson.fromJson(json, FailedRegistrationNotifyEvent::class.java)
                        logger.info("Got failed registration event: $failedRegistrationNotifyEvent")
                        handleFailedRegistration(failedRegistrationNotifyEvent, serviceId)
                    }
                    // Handle 'got enough proofs' in Ethereum
                    EthWithdrawalProofsEvent::class.java.canonicalName -> {
                        val ethWithdrawalProofsEvent =
                            gson.fromJson(json, EthWithdrawalProofsEvent::class.java)
                        logger.info("Got 'enough proofs' event: $ethWithdrawalProofsEvent")
                        handleEthWithdrawalProofs(ethWithdrawalProofsEvent, serviceId)
                    }
                    else -> logger.warn("Cannot handle event type $eventType. Message is $json")
                }
                ack(delivery)
            } catch (e: Exception) {
                handleError(delivery, serviceId, e)
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
        notificationServices.forEach { notificationService ->
            val messageProperties = MessageProperties.MINIMAL_PERSISTENT_BASIC.builder()
                .headers(
                    mapOf(
                        Pair(EVENT_TYPE_HEADER, event.javaClass.canonicalName),
                        Pair(NOTIFICATION_SERVICE_ID_HEADER, notificationService.serviceId()),
                        Pair(DEDUPLICATION_HEADER, event.id + "_" + notificationService.serviceId())
                    )
                ).build()
            try {
                logger.info("Enqueue event $event to queue $queue. Service is ${notificationService.serviceId()}")
                val json = gson.toJson(event)
                channel.basicPublish(
                    "",
                    queue,
                    messageProperties,
                    json.toByteArray()
                )
                logger.info("Event $event has been successfully published to queue $queue. Service is ${notificationService.serviceId()}")
            } catch (e: Exception) {
                logger.error("Cannot enqueue $event. Service is ${notificationService.serviceId()}", e)
            }
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
     * @param serviceId - id of the service that must handle the event
     * @param iterator - iteration logic
     */
    private fun iterateThroughNotificationServices(
        serviceId: String,
        iterator: (NotificationService) -> Result<Unit, Exception>
    ) {
        notificationServices.filter { it.serviceId() == serviceId }.take(1)
            .forEach { iterator(it).failure { ex -> throw ex } }
    }

    /**
     * Handles event processing errors
     * @param delivery - delivery to handle
     * @param serviceId - id of service that is responsible for handling
     * @param error - error to handle
     */
    private fun handleError(delivery: Delivery, serviceId: String, error: Exception) {
        val json = String(delivery.body)
        if (error is RepeatableException) {
            // Re-queue if possible
            logger.warn("Cannot handle event due to error. Try to re-queue. Json $json. Service id $serviceId", error)
            nack(delivery)
        } else {
            ack(delivery)
            logger.error("Cannot handle delivery $json. Service id $serviceId")
        }
    }

    // Acknowledges a given RMQ message
    private fun ack(delivery: Delivery) {
        if (!autoAck) {
            logger.info("Ack message ${String(delivery.body)}")
            channel.basicAck(delivery.envelope.deliveryTag, false)
        }
    }

    // Negatively acknowledges a given RMQ message and hints RMQ to re-queue it
    private fun nack(delivery: Delivery) {
        if (!autoAck) {
            logger.info("Nack message ${String(delivery.body)}")
            channel.basicNack(delivery.envelope.deliveryTag, false, requeue)
        }
    }

    /**
     * Handles registration events from RabbitMQ
     * @param registrationNotifyEvent - event to handle
     * @param serviceId - id of the service that must handle the event
     */
    private fun handleRegistration(registrationNotifyEvent: RegistrationNotifyEvent, serviceId: String) {
        iterateThroughNotificationServices(serviceId)
        { it.notifyRegistration(registrationNotifyEvent) }
    }

    /**
     * Handles 'got enough proofs for withdrawal' in the Ethereum subsystem
     * @param ethWithdrawalProofsEvent - event ot handle
     * @param serviceId - id of the service that must handle the event
     */
    private fun handleEthWithdrawalProofs(ethWithdrawalProofsEvent: EthWithdrawalProofsEvent, serviceId: String) {
        notificationServices.filter { it.serviceId() == serviceId }.take(1).forEach {
            if (it is EthSpecificNotificationService) {
                it.notifyEthWithdrawalProofs(ethWithdrawalProofsEvent).failure { ex -> throw ex }
            }
        }
    }

    /**
     * Handles failed registration events from RabbitMQ
     * @param failedRegistrationNotifyEvent - event to handle
     * @param serviceId - id of the service that must handle the event
     */
    private fun handleFailedRegistration(
        failedRegistrationNotifyEvent: FailedRegistrationNotifyEvent,
        serviceId: String
    ) {
        iterateThroughNotificationServices(serviceId)
        { it.notifyFailedRegistration(failedRegistrationNotifyEvent) }
    }

    /**
     * Handles deposit transfer from RabbitMQ
     * @param transferNotifyEvent - deposit event to handle
     * @param serviceId - id of the service that must handle the event
     */
    private fun handleDepositTransfer(transferNotifyEvent: DepositTransferEvent, serviceId: String) {
        iterateThroughNotificationServices(serviceId) { it.notifyDeposit(transferNotifyEvent) }
    }

    /**
     * Handles 'receive' transfer from RabbitMQ
     * @param transferNotifyEvent - 'receive' event to handle
     * @param serviceId - id of the service that must handle the event
     */
    private fun handleReceiveTransfer(transferNotifyEvent: Client2ClientReceiveTransferEvent, serviceId: String) {
        iterateThroughNotificationServices(serviceId) {
            it.notifyReceiveFromClient(transferNotifyEvent)
        }
    }

    /**
     * Handles 'send' transfer from RabbitMQ
     * @param transferNotifyEvent - 'send' event to handle
     * @param serviceId - id of the service that must handle the event
     */
    private fun handleSendTransfer(transferNotifyEvent: Client2ClientSendTransferEvent, serviceId: String) {
        iterateThroughNotificationServices(serviceId) { it.notifySendToClient(transferNotifyEvent) }
    }

    companion object : KLogging()
}
