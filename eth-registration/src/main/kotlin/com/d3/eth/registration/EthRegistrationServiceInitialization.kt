package com.d3.eth.registration

import com.d3.commons.config.EthereumPasswords
import com.d3.commons.model.IrohaCredential
import com.d3.commons.registration.RegistrationServiceEndpoint
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.eth.provider.EthFreeRelayProvider
import com.d3.eth.provider.EthRelayProviderIrohaImpl
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import mu.KLogging

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
