package com.d3.notifications.debug

import com.d3.notifications.config.NotificationsConfig
import com.d3.notifications.debug.dto.TO_HEADER
import com.d3.notifications.debug.dto.mapDumbsterMessage
import com.d3.notifications.event.*
import com.dumbster.smtp.SimpleSmtpServer
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.response.respond
import io.ktor.routing.get
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
    private val soraFailedRegistrationEvents = Collections.synchronizedList(ArrayList<SoraFailedRegistrationEvent>())
    private val soraWithdrawalProofsEvents = Collections.synchronizedList(ArrayList<SoraEthWithdrawalProofsEvent>())

    /**
     * Initiates ktor based HTTP server
     */
    init {
        server = embeddedServer(Netty, port = notificationsConfig.debugWebPort) {
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
            }
        }
        server.start(wait = false)
    }

    override fun close() {
        server.stop(5, 5, TimeUnit.SECONDS)
    }
}
