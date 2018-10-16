package registration.eth

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import config.EthereumPasswords
import model.IrohaCredential
import mu.KLogging
import provider.eth.EthFreeRelayProvider
import registration.RegistrationServiceEndpoint
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.ModelUtil

/**
 * Initialisation of Registration Service
 *
 * @param ethRegistrationConfig - configurations of registration service
 */
class EthRegistrationServiceInitialization(
    private val ethRegistrationConfig: EthRegistrationConfig,
    private val passwordConfig: EthereumPasswords,
    private val irohaNetwork: IrohaNetwork
) {

    /**
     * Init Registration Service
     */
    fun init(): Result<Unit, Exception> {

        logger.info {
            "Start registration service init with iroha creator: ${ethRegistrationConfig.registrationCredential.accountId}"
        }

        return ModelUtil.loadKeypair(
            ethRegistrationConfig.registrationCredential.pubkeyPath,
            ethRegistrationConfig.registrationCredential.privkeyPath
        ).map { keypair -> IrohaCredential(ethRegistrationConfig.registrationCredential.accountId, keypair) }
            .map { credential ->
                Pair(
                    EthFreeRelayProvider(
                        credential,
                        ethRegistrationConfig.notaryIrohaAccount,
                        ethRegistrationConfig.relayRegistrationIrohaAccount,
                        irohaNetwork
                    ),
                    IrohaConsumerImpl(credential, irohaNetwork)
                )
            }
            .map { (ethFreeRelayProvider, irohaConsumer) ->
                EthRegistrationStrategyImpl(
                    ethFreeRelayProvider,
                    ethRegistrationConfig,
                    passwordConfig,
                    irohaConsumer,
                    ethRegistrationConfig.notaryIrohaAccount
                )
            }.map { registrationStrategy ->
                RegistrationServiceEndpoint(
                    ethRegistrationConfig.port,
                    registrationStrategy
                )
            }.map { Unit }
    }

    /**
     * Logger
     */
    companion object : KLogging()

}
