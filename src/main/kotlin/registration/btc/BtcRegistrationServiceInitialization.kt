package registration.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import registration.RegistrationServiceEndpoint
import sidechain.iroha.consumer.IrohaConsumerImpl

class BtcRegistrationServiceInitialization(private val btcRegistrationConfig: BtcRegistrationConfig) {
    /**
     * Init Registration Service
     */
    fun init(): Result<Unit, Exception> {
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
}

