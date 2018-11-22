package healthcheck

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component
class ServiceInitHealthCheck(
    @Autowired
    private val healthyService: HealthyService
) : HealthIndicator {

    override fun health(): Health {
        if (healthyService.isHealthy()) {
            return Health.up().build()
        } else {
            return Health.down().build()
        }
    }
}
