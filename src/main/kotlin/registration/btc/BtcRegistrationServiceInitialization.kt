package registration.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import mu.KLogging
import registration.RegistrationServiceEndpoint
import sidechain.iroha.consumer.IrohaConsumerImpl

class BtcRegistrationServiceInitialization(private val btcRegistrationConfig: BtcRegistrationConfig) {
    /**
     * Init Registration Service
     */
    fun init(): Result<Unit, Exception> {
        logger.info { "Init BTC client registration service" }
        return Result.of {
            val irohaConsumer = IrohaConsumerImpl(btcRegistrationConfig.iroha)
            BtcRegistrationStrategyImpl(
                irohaConsumer,
                btcRegistrationConfig.iroha.creator,
                btcRegistrationConfig.registrationAccount,
                btcRegistrationConfig.btcWalletPath
            )
        }.map { registrationStrategy ->
            RegistrationServiceEndpoint(
                btcRegistrationConfig.port,
                registrationStrategy
            )
            Unit
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}

