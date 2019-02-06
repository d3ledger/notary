package integration.sora

import config.loadConfigs
import integration.helper.IrohaIntegrationHelperUtil
import model.IrohaCredential
import registration.NotaryRegistrationConfig
import registration.NotaryRegistrationStrategy
import registration.RegistrationServiceInitialization
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil

/**
 * Environment for registration service running in tests
 */
class RegistrationServiceTestEnvironment(private val integrationHelper: IrohaIntegrationHelperUtil) {

    val registrationConfig =
        loadConfigs("registration", NotaryRegistrationConfig::class.java, "/registration.properties").get()

    private val registrationCredentials = ModelUtil.loadKeypair(
        registrationConfig.registrationCredential.pubkeyPath,
        registrationConfig.registrationCredential.privkeyPath
    ).fold(
        { keypair ->
            IrohaCredential(registrationConfig.registrationCredential.accountId, keypair)
        },
        { ex -> throw ex }
    )

    private val irohaConsumer = IrohaConsumerImpl(registrationCredentials, integrationHelper.irohaAPI)

    private val registrationStrategy = NotaryRegistrationStrategy(irohaConsumer)

    val registrationInitialization = RegistrationServiceInitialization(registrationConfig, registrationStrategy)

}
