package com.d3.btc.healthcheck

import java.util.concurrent.atomic.AtomicBoolean

/*
    Class that is used to tell if service is working successfully
 */
open class HealthyService {
    // Service is considered properly initialized by default
    private val healthy = AtomicBoolean(true)

    // Must be called to say that service is not in healthy condition
    fun notHealthy() {
        healthy.set(false)
    }

    // Must be called to say that service is working fine again
    fun cured() {
        healthy.set(true)
    }

    // Returns service health status
    fun isHealthy() = healthy.get()
}
