package com.d3.btc.registration.config

import com.d3.btc.provider.BtcFreeAddressesProvider
import com.d3.btc.provider.BtcRegisteredAddressesProvider
import com.d3.btc.provider.account.IrohaBtcAccountCreator
import com.d3.btc.provider.address.BtcAddressesProvider
import com.d3.commons.config.loadConfigs
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import com.d3.commons.model.IrohaCredential
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil

val btcRegistrationConfig =
    loadConfigs("btc-registration", BtcRegistrationConfig::class.java, "/btc/registration.properties").get()

@Configuration
class BtcRegistrationAppConfiguration {

    private val btcRegistrationCredential = ModelUtil.loadKeypair(
        btcRegistrationConfig.registrationCredential.pubkeyPath,
        btcRegistrationConfig.registrationCredential.privkeyPath
    ).fold(
        { keypair ->
            IrohaCredential(btcRegistrationConfig.registrationCredential.accountId, keypair)
        },
        { ex -> throw ex }
    )

    @Bean
    fun irohaAPI() = IrohaAPI(btcRegistrationConfig.iroha.hostname, btcRegistrationConfig.iroha.port)

    @Bean
    fun queryAPI() = QueryAPI(
        irohaAPI(),
        btcRegistrationCredential.accountId,
        btcRegistrationCredential.keyPair
    )

    @Bean
    fun btcRegistrationConfig() = btcRegistrationConfig

    @Bean
    fun btcFreeAddressesProvider(): BtcFreeAddressesProvider {
        return BtcFreeAddressesProvider(
            btcRegistrationConfig.nodeId,
            BtcAddressesProvider(
                queryAPI(),
                btcRegistrationConfig.mstRegistrationAccount,
                btcRegistrationConfig.notaryAccount
            ),
            BtcRegisteredAddressesProvider(
                queryAPI(),
                btcRegistrationCredential.accountId,
                btcRegistrationConfig.notaryAccount
            )
        )
    }

    @Bean
    fun btcRegistrationConsumer() = IrohaConsumerImpl(btcRegistrationCredential, irohaAPI())

    @Bean
    fun irohaBtcAccountCreator(): IrohaBtcAccountCreator {
        return IrohaBtcAccountCreator(
            btcRegistrationConsumer(),
            btcRegistrationConfig.notaryAccount,
            btcRegistrationConfig.clientStorageAccount
        )
    }
}
