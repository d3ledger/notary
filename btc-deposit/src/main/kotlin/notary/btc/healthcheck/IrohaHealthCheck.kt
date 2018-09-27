package notary.btc.healthcheck

import config.IrohaConfig
import jp.co.soramitsu.iroha.Keypair
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.getAssetInfo

@Component
class IrohaHealthCheck(
    @Autowired
    @Qualifier("healthCheckIrohaConfig")
    private val irohaConfig: IrohaConfig,
    @Autowired
    @Qualifier("healthCheckKeyPair")
    private val keypair: Keypair
) : HealthIndicator {

    private val irohaNetwork = IrohaNetworkImpl(irohaConfig.hostname, irohaConfig.port)

    override fun health(): Health {
        return getAssetInfo(irohaConfig, keypair, irohaNetwork, "ether#ethereum").fold(
            { Health.up().build() },
            { ex ->
                logger.error("Health check fail", ex)
                Health.down().build()
            })
    }

    /**
     * Logger
     */
    companion object : KLogging()

}
