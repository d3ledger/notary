/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.btc.healthcheck

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

/**
 * Health check endpoint
 */
@Component
class HealthCheckEndpoint(
    @Qualifier("healthCheckPort")
    @Autowired private val healthCheckPort: Int,
    @Autowired private val serviceInitHealthCheck: ServiceInitHealthCheck
) {

    /**
     * Initiates ktor based helath check server
     */
    init {
        val server = embeddedServer(Netty, port = healthCheckPort) {
            install(CORS)
            {
                anyHost()
            }
            routing {
                get("/health") {
                    val (message, status) = getHealth()
                    call.respond(status, message)
                }
            }
        }
        server.start(wait = false)
    }

    /**
     * Returns health status in a form <message(UP or DOWN), HTTP status(200 or 500)>
     */
    private fun getHealth(): Pair<String, HttpStatusCode> {
        return if (serviceInitHealthCheck.isHealthy()) {
            Pair("UP", HttpStatusCode.OK)
        } else {
            Pair("DOWN", HttpStatusCode.InternalServerError)
        }
    }
}
