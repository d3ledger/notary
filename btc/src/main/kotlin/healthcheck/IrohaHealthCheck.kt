package healthcheck

import model.IrohaCredential
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.getAssetInfo

@Component
class IrohaHealthCheck(
    @Autowired
    @Qualifier("irohaHealthCheckCredential")
    private val irohaHealthCheckCredential: IrohaCredential,
    @Autowired
    private val irohaNetwork: IrohaNetwork
) : HealthIndicator {
    override fun health(): Health {
        return getAssetInfo(irohaHealthCheckCredential, irohaNetwork, "ether#ethereum").fold(
            { Health.up().build() },
            { ex ->
                logger.error("Iroha health check fail", ex)
                Health.down().build()
            })
    }

    /**
     * Logger
     */
    companion object : KLogging()

}
