package registration.btc

import com.github.kittinunf.result.Result
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import registration.RegistrationServiceEndpoint
import registration.RegistrationStrategy
import registration.btc.config.BtcRegistrationConfig
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetwork

@Component
class BtcRegistrationServiceInitialization(
    @Autowired private val btcRegistrationConfig: BtcRegistrationConfig,
    @Autowired private val btcRegistrationStrategy: RegistrationStrategy
) {
    /**
     * Init Registration Service
     */
    fun init(): Result<Unit, Exception> {
        logger.info { "Init BTC client registration service" }
        return Result.of {
            RegistrationServiceEndpoint(
                btcRegistrationConfig.port,
                btcRegistrationStrategy
            )
            Unit
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}

