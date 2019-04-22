package com.d3.btc.generation.trigger

import com.d3.btc.generation.config.BtcAddressGenerationConfig
import com.d3.btc.provider.BtcFreeAddressesProvider
import com.d3.btc.provider.BtcRegisteredAddressesProvider
import com.d3.btc.provider.address.BtcAddressesProvider
import com.d3.btc.provider.generation.BtcSessionProvider
import com.d3.commons.config.loadConfigs
import com.d3.commons.model.IrohaCredential
import com.d3.commons.provider.TriggerProvider
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import jp.co.soramitsu.iroha.java.IrohaAPI
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

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
        IrohaCredential(
            btcAddressGenerationTriggerConfig.registrationAccount.accountId,
            registrationKeyPair
        )

    @Bean
    fun triggerIrohaAPI() =
        IrohaAPI(
            btcAddressGenerationTriggerConfig.iroha.hostname,
            btcAddressGenerationTriggerConfig.iroha.port
        )

    @Bean
    fun registrationQueryHelper() = IrohaQueryHelperImpl(
        triggerIrohaAPI(),
        registrationCredential.accountId,
        registrationKeyPair
    )

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
            registrationQueryHelper(),
            btcAddressGenerationTriggerConfig.mstRegistrationAccount.accountId,
            btcAddressGenerationTriggerConfig.notaryAccount
        )
    }

    @Bean
    fun btcRegisteredAddressesProvider(): BtcRegisteredAddressesProvider {
        return BtcRegisteredAddressesProvider(
            registrationQueryHelper(),
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
