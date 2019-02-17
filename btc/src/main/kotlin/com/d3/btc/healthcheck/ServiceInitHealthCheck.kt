package com.d3.btc.healthcheck

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component
class ServiceInitHealthCheck(
    @Autowired
    private val healthyServices: List<HealthyService>
) : HealthIndicator {

    override fun health(): Health {
        healthyServices.forEach { healthyService ->
            if (!healthyService.isHealthy()) {
                return Health.down().build()
            }
        }
        return Health.up().build()
    }
}
