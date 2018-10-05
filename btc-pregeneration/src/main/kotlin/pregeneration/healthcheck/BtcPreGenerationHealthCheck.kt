package pregeneration.healthcheck

import healthcheck.HealthyServiceInitializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component
class BtcPreGenerationHealthCheck(
    @Autowired
    private val btcPreGenInitialization: HealthyServiceInitializer
) : HealthIndicator {

    override fun health(): Health {
        if (btcPreGenInitialization.isHealthy()) {
            return Health.up().build()
        } else {
            return Health.down().build()
        }
    }
}
