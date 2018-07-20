package registration

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil

/**
 * Initialisation of Registration Service
 *
 * @param registrationConfig - configurations of registration service
 */
class RegistrationServiceInitialization(val registrationConfig: RegistrationConfig) {

    /**
     * Init Registration Service
     */
    fun init(): Result<Unit, Exception> {
        return Result.of {
            ModelUtil.loadKeypair(
                registrationConfig.iroha.pubkeyPath,
                registrationConfig.iroha.privkeyPath
            )
                .map {
                    Pair(
                        EthFreeWalletsProvider(
                            registrationConfig.iroha,
                            it,
                            registrationConfig.notaryIrohaAccount,
                            registrationConfig.relayRegistrationIrohaAccount
                        ), IrohaConsumerImpl(registrationConfig.iroha)
                    )
                }
                .map { (ethFreeWalletsProvider, irohaConsumer) ->
                    RegistrationStrategyImpl(
                        ethFreeWalletsProvider,
                        irohaConsumer,
                        registrationConfig.notaryIrohaAccount,
                        registrationConfig.iroha
                    )
                }
                .map { RegistrationServiceEndpoint(registrationConfig.port, it) }
            Unit
        }
    }

}
