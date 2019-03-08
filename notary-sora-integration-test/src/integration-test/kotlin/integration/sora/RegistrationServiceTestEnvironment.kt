package integration.sora

import com.d3.commons.config.loadConfigs
import integration.helper.IrohaIntegrationHelperUtil
import com.d3.commons.model.IrohaCredential
import com.d3.commons.registration.NotaryRegistrationConfig
import com.d3.commons.registration.NotaryRegistrationStrategy
import com.d3.commons.registration.RegistrationServiceInitialization
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil

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

    val registrationInitialization =
        RegistrationServiceInitialization(registrationConfig, registrationStrategy)

}
