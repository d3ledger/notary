package registration.eth

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import provider.eth.EthFreeRelayProvider
import registration.RegistrationServiceEndpoint
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil

/**
 * Initialisation of Registration Service
 *
 * @param ethRegistrationConfig - configurations of registration service
 */
class EthRegistrationServiceInitialization(private val ethRegistrationConfig: EthRegistrationConfig) {

    /**
     * Init Registration Service
     */
    fun init(): Result<Unit, Exception> {
        return Result.of {
            ModelUtil.loadKeypair(
                ethRegistrationConfig.iroha.pubkeyPath,
                ethRegistrationConfig.iroha.privkeyPath
            )
                .map { keyPair ->
                    Pair(
                        EthFreeRelayProvider(
                            ethRegistrationConfig.iroha,
                            keyPair,
                            ethRegistrationConfig.notaryIrohaAccount,
                            ethRegistrationConfig.relayRegistrationIrohaAccount
                        ), IrohaConsumerImpl(ethRegistrationConfig.iroha)
                    )
                }
                .map { (ethFreeRelayProvider, irohaConsumer) ->
                    EthRegistrationStrategyImpl(
                        ethFreeRelayProvider,
                        irohaConsumer,
                        ethRegistrationConfig.notaryIrohaAccount,
                        ethRegistrationConfig.iroha.creator
                    )
                }
                .map { registrationStrategy ->
                    RegistrationServiceEndpoint(
                        ethRegistrationConfig.port,
                        registrationStrategy
                    )
                }
            Unit
        }
    }

}
