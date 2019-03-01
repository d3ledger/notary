package registration.eth

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import config.EthereumPasswords
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import model.IrohaCredential
import mu.KLogging
import provider.eth.EthFreeRelayProvider
import provider.eth.EthRelayProviderIrohaImpl
import com.d3.registration.RegistrationServiceEndpoint
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil

/**
 * Initialisation of Registration Service
 *
 * @param ethRegistrationConfig - configurations of registration service
 */
class EthRegistrationServiceInitialization(
    private val ethRegistrationConfig: EthRegistrationConfig,
    private val passwordConfig: EthereumPasswords,
    private val irohaAPI: IrohaAPI
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
                val queryAPI = QueryAPI(irohaAPI, credential.accountId, credential.keyPair)
                Pair(
                    Pair(
                        EthFreeRelayProvider(
                            queryAPI,
                            ethRegistrationConfig.notaryIrohaAccount,
                            ethRegistrationConfig.relayRegistrationIrohaAccount
                        ),
                        EthRelayProviderIrohaImpl(
                            queryAPI,
                            ethRegistrationConfig.notaryIrohaAccount,
                            ethRegistrationConfig.relayRegistrationIrohaAccount
                        )
                    ),
                    IrohaConsumerImpl(credential, irohaAPI)
                )
            }
            .map { (providers, irohaConsumer) ->
                val (ethFreeRelayProvider, ethRelayProvider) = providers
                EthRegistrationStrategyImpl(
                    ethFreeRelayProvider,
                    ethRelayProvider,
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
