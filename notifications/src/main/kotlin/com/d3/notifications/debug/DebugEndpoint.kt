package com.d3.notifications.debug

import com.d3.notifications.config.NotificationsConfig
import com.d3.notifications.debug.dto.TO_HEADER
import com.d3.notifications.debug.dto.mapDumbsterMessage
import com.d3.notifications.event.*
import com.d3.notifications.service.*
import com.dumbster.smtp.SimpleSmtpServer
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.io.Closeable
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

/**
 * HTTP endpoint for debug purposes.
 */
class DebugEndpoint(
    private val dumbster: SimpleSmtpServer,
    notificationsConfig: NotificationsConfig
) : Closeable {

    private val server: ApplicationEngine
    private val soraDepositEvents = Collections.synchronizedList(ArrayList<SoraDepositEvent>())
    private val soraWithdrawalEvents = Collections.synchronizedList(ArrayList<SoraWithdrawalEvent>())
    private val soraRegistrationEvents = Collections.synchronizedList(ArrayList<SoraRegistrationEvent>())
    private val soraTransferSendEvents = Collections.synchronizedList(ArrayList<SoraTransferEventSend>())
    private val soraTransferReceiveEvents = Collections.synchronizedList(ArrayList<SoraTransferEventReceive>())

    /**
     * Initiates ktor based HTTP server
     */
    init {
        server = embeddedServer(Netty, port = notificationsConfig.webPort) {
            install(CORS)
            {
                anyHost()
            }
            install(ContentNegotiation) {
                gson()
            }
            routing {
                /**
                 * Returns all the messages in the dumbster inbox
                 */
                get("/dumbster/mail/all") {
                    val to = call.parameters["to"]
                    val mails = if (to == null) {
                        // Return all messages if 'to' header is not specified
                        dumbster.receivedEmails.map { mapDumbsterMessage(it) }
                    } else {
                        // Return all messages with specified recipient
                        dumbster.receivedEmails
                            .filter { it.getHeaderValue(TO_HEADER) == to }
                            .map { mapDumbsterMessage(it) }
                    }
                    call.respond(mails)
                }
                /**
                 * Returns all the events for the Sora subsystem
                 */
                get("/sora/all/{eventType}") {
                    val eventType = call.parameters["eventType"]
                    call.respond(
                        when (eventType) {
                            "transferSend" -> soraTransferSendEvents
                            "transferReceive" -> soraTransferReceiveEvents
                            "deposit" -> soraDepositEvents
                            "registration" -> soraRegistrationEvents
                            "withdrawal" -> soraWithdrawalEvents
                            else -> ArrayList<SoraEvent>()
                        }
                    )
                }
                post("/sora/$DEPOSIT_URI") {
                    val depositEvent = call.receive<SoraDepositEvent>()
                    soraDepositEvents.add(depositEvent)
                    call.respond("Ok")
                }
                post("/sora/$WITHDRAWAL_URI") {
                    val withdrawalEvent = call.receive<SoraWithdrawalEvent>()
                    soraWithdrawalEvents.add(withdrawalEvent)
                    call.respond("Ok")
                }
                post("/sora/$TRANSFER_RECEIVE_URI") {
                    val transferEventReceive = call.receive<SoraTransferEventReceive>()
                    soraTransferReceiveEvents.add(transferEventReceive)
                    call.respond("Ok")
                }
                post("/sora/$TRANSFER_SEND_URI") {
                    val transferEventSend = call.receive<SoraTransferEventSend>()
                    soraTransferSendEvents.add(transferEventSend)
                    call.respond("Ok")
                }
                post("/sora/$REGISTRATION_URI") {
                    val registrationEvent = call.receive<SoraRegistrationEvent>()
                    soraRegistrationEvents.add(registrationEvent)
                    call.respond("Ok")
                }
            }
        }
        server.start(wait = false)
    }

    override fun close() {
        server.stop(5, 5, TimeUnit.SECONDS)
    }
}
