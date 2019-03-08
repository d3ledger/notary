package com.d3.eth.withdrawal.withdrawalservice

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KLogging


/**
 * Withdrawal HTTP service
 */
class WithdrawalServiceEndpoint(
    port: Int
) {

    init {

        logger.info { "Start withdrawal server on port $port" }

        val server = embeddedServer(Netty, port = port) {
            install(CORS)
            {
                anyHost()
                allowCredentials = true
            }
            install(ContentNegotiation) {
                gson()
            }
            routing {
                get("/actuator/health") {
                    call.respond(
                        mapOf(
                            "status" to "UP"
                        )
                    )
                }
            }
        }
        server.start(wait = false)
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
