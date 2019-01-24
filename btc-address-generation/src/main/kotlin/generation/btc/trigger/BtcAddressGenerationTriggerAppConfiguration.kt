package generation.btc.trigger

import config.loadConfigs
import generation.btc.config.BtcAddressGenerationConfig
import model.IrohaCredential
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import provider.TriggerProvider
import provider.btc.generation.BtcSessionProvider
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil

val btcAddressGenerationTriggerConfig =
    loadConfigs(
        "btc-address-generation",
        BtcAddressGenerationConfig::class.java,
        "/btc/address_generation.properties"
    ).get()

@Configuration
class BtcAddressGenerationTriggerAppConfiguration {

    private val registrationKeyPair =
        ModelUtil.loadKeypair(
            btcAddressGenerationTriggerConfig.registrationAccount.pubkeyPath,
            btcAddressGenerationTriggerConfig.registrationAccount.privkeyPath
        ).fold({ keypair ->
            keypair
        }, { ex -> throw ex })

    private val registrationCredential =
        IrohaCredential(btcAddressGenerationTriggerConfig.registrationAccount.accountId, registrationKeyPair)

    @Bean
    fun irohaNetwork() =
        IrohaNetworkImpl(
            btcAddressGenerationTriggerConfig.iroha.hostname,
            btcAddressGenerationTriggerConfig.iroha.port
        )

    @Bean
    fun btcSessionProvider() =
        BtcSessionProvider(registrationCredential, irohaNetwork())

    @Bean
    fun triggerProvider() =
        TriggerProvider(registrationCredential, irohaNetwork(), btcAddressGenerationTriggerConfig.pubKeyTriggerAccount)
}
