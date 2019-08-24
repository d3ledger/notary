package com.d3.notifications.rest

import com.d3.notifications.config.NotificationsConfig
import com.d3.notifications.rest.dto.TO_HEADER
import com.d3.notifications.rest.dto.mapDumbsterMessage
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
import org.springframework.stereotype.Component
import java.io.Closeable
import java.util.concurrent.TimeUnit


/**
 * HTTP endpoint for dummy 'dumbster' SMTP server. Good for testing.
 */
@Component
class DumbsterEndpoint(
    private val dumbster: SimpleSmtpServer,
    notificationsConfig: NotificationsConfig
) : Closeable {

    private val server: ApplicationEngine

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
            }
        }
        server.start(wait = false)
    }

    override fun close() {
        server.stop(5, 5, TimeUnit.SECONDS)
    }
}
