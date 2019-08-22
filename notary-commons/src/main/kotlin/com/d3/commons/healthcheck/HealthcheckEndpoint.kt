/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.healthcheck

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
import java.util.concurrent.TimeUnit

class HealthCheckEndpoint(healthCheckPort: Int) : Closeable {

    private val server: ApplicationEngine

    /**
     * Initiates ktor based health check server
     */
    init {
        server = embeddedServer(Netty, port = healthCheckPort) {
            install(CORS)
            {
                anyHost()
            }
            install(ContentNegotiation) {
                gson()
            }
            routing {
                get("/actuator/health") {
                    call.respond(mapOf("status" to "UP"))
                }
            }
        }
        server.start(wait = false)
    }

    override fun close() {
        server.stop(5, 5, TimeUnit.SECONDS)
    }

}
