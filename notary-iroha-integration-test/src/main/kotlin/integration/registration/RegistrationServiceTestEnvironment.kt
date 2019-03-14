package integration.registration

import com.d3.commons.model.IrohaCredential
import com.d3.commons.registration.NotaryRegistrationStrategy
import com.d3.commons.registration.RegistrationServiceInitialization
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import integration.helper.IrohaIntegrationHelperUtil
import java.io.Closeable

/**
 * Environment for registration service running in tests
 */
class RegistrationServiceTestEnvironment(private val integrationHelper: IrohaIntegrationHelperUtil) : Closeable {

    val registrationConfig = integrationHelper.configHelper.createRegistrationConfig(integrationHelper.accountHelper)

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

    override fun close() {
        integrationHelper.close()
    }
}
