package com.d3.btc.generation.trigger

import com.d3.btc.provider.BtcFreeAddressesProvider
import com.d3.btc.provider.BtcRegisteredAddressesProvider
import com.d3.btc.provider.address.BtcAddressesProvider
import com.d3.btc.provider.generation.BtcSessionProvider
import com.d3.commons.config.loadConfigs
import com.d3.btc.generation.config.BtcAddressGenerationConfig
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import com.d3.commons.model.IrohaCredential
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import com.d3.commons.provider.TriggerProvider
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil

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
