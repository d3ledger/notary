package registration.btc.config

import config.loadConfigs
import model.IrohaCredential
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import provider.btc.address.BtcAddressesProvider
import provider.btc.address.BtcRegisteredAddressesProvider
import provider.btc.account.IrohaBtcAccountCreator
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil

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
    fun irohaNetwork() = IrohaNetworkImpl(btcRegistrationConfig.iroha.hostname, btcRegistrationConfig.iroha.port)

    @Bean
    fun btcClientCreatorConsumer() = IrohaConsumerImpl(
        btcRegistrationCredential, irohaNetwork()
    )

    @Bean
    fun btcRegistrationConfig() = btcRegistrationConfig

    @Bean
    fun btcAddressesProvider(): BtcAddressesProvider {
        return BtcAddressesProvider(
            btcRegistrationCredential,
            irohaNetwork(),
            btcRegistrationConfig.mstRegistrationAccount,
            btcRegistrationConfig.notaryAccount
        )
    }

    @Bean
    fun btcRegisteredAddressesProvider(): BtcRegisteredAddressesProvider {
        return BtcRegisteredAddressesProvider(
            btcRegistrationCredential,
            irohaNetwork(),
            btcRegistrationCredential.accountId,
            btcRegistrationConfig.notaryAccount
        )
    }

    @Bean
    fun irohaBtcAccountCreator(): IrohaBtcAccountCreator {
        return IrohaBtcAccountCreator(
            btcClientCreatorConsumer(),
            btcRegistrationConfig.notaryAccount
        )
    }
}
