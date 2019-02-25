package com.d3.btc.healthcheck

import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ServiceInitHealthCheck(
    @Autowired
    private val healthyServices: List<HealthyService>
) {
    init {
        if (healthyServices.isEmpty()) {
            logger.warn("No healthy services were set. Health check will always return 'UP'")
        }
    }

    fun isHealthy(): Boolean {
        healthyServices.forEach { healthyService ->
            if (!healthyService.isHealthy()) {
                return false
            }
        }
        return true
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
