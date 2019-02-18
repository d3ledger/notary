package generation.btc.trigger

import com.d3.btc.provider.BtcFreeAddressesProvider
import com.d3.btc.provider.BtcRegisteredAddressesProvider
import com.d3.btc.provider.address.BtcAddressesProvider
import com.d3.btc.provider.generation.BtcSessionProvider
import config.loadConfigs
import generation.btc.config.BtcAddressGenerationConfig
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import model.IrohaCredential
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import provider.TriggerProvider
import sidechain.iroha.consumer.IrohaConsumerImpl
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
    fun triggerIrohaAPI() =
        IrohaAPI(
            btcAddressGenerationTriggerConfig.iroha.hostname,
            btcAddressGenerationTriggerConfig.iroha.port
        )

    @Bean
    fun registrationQueryAPI() = QueryAPI(triggerIrohaAPI(), registrationCredential.accountId, registrationKeyPair)

    @Bean
    fun addressGenerationConsumer() = IrohaConsumerImpl(registrationCredential, triggerIrohaAPI())

    @Bean
    fun btcSessionProvider() =
        BtcSessionProvider(registrationCredential, triggerIrohaAPI())

    @Bean
    fun triggerProvider() =
        TriggerProvider(
            registrationCredential,
            triggerIrohaAPI(),
            btcAddressGenerationTriggerConfig.pubKeyTriggerAccount
        )

    @Bean
    fun btcAddressesProvider(): BtcAddressesProvider {
        return BtcAddressesProvider(
            registrationQueryAPI(),
            btcAddressGenerationTriggerConfig.mstRegistrationAccount.accountId,
            btcAddressGenerationTriggerConfig.notaryAccount
        )
    }

    @Bean
    fun btcRegisteredAddressesProvider(): BtcRegisteredAddressesProvider {
        return BtcRegisteredAddressesProvider(
            registrationQueryAPI(),
            registrationCredential.accountId,
            btcAddressGenerationTriggerConfig.notaryAccount
        )
    }

    @Bean
    fun btcFreeAddressesProvider() =
        BtcFreeAddressesProvider(
            btcAddressGenerationTriggerConfig.nodeId,
            btcAddressesProvider(),
            btcRegisteredAddressesProvider()
        )
}
