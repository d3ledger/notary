package registration

import config.loadConfigs
import model.IrohaCredential
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil

val registrationConfig =
    loadConfigs("registration", NotaryRegistrationConfig::class.java, "/registration.properties").get()

/**
 * Spring configuration for Notary Registration Service
 */
@Configuration
class NotaryRegistrationAppConfiguration {

    /** Registartion service credentials */
    private val registrationCredential = ModelUtil.loadKeypair(
        registrationConfig.registrationCredential.pubkeyPath,
        registrationConfig.registrationCredential.privkeyPath
    ).fold(
        { keypair ->
            IrohaCredential(registrationConfig.registrationCredential.accountId, keypair)
        },
        { ex -> throw ex }
    )

    /** Iroha network connection */
    @Bean
    fun irohaNetwork() = IrohaNetworkImpl(registrationConfig.iroha.hostname, registrationConfig.iroha.port)

    @Bean
    fun irohaConsumer() = IrohaConsumerImpl(
        registrationCredential, irohaNetwork()
    )

    /** Configurations for Notary Registration Service */
    @Bean
    fun registrationConfig() = registrationConfig

}
