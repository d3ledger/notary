package registration.btc.config

import config.loadConfigs
import model.IrohaCredential
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import provider.btc.address.BtcAddressesProvider
import provider.btc.address.BtcRegisteredAddressesProvider
import registration.IrohaAccountCreator
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil

val btcRegistrationConfig =
    loadConfigs("btc-registration", BtcRegistrationConfig::class.java, "/btc/registration.properties")

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
    private val btcClientCreatorConsumer = IrohaConsumerImpl(btcRegistrationCredential, btcRegistrationConfig.iroha)

    @Bean
    fun btcRegistrationConfig() = btcRegistrationConfig

    @Bean
    fun btcAddressesProvider(): BtcAddressesProvider {
        return BtcAddressesProvider(
            btcRegistrationConfig.iroha,
            btcRegistrationCredential,
            btcRegistrationConfig.mstRegistrationAccount,
            btcRegistrationConfig.notaryAccount
        )
    }

    @Bean
    fun btcRegisteredAddressesProvider(): BtcRegisteredAddressesProvider {
        return BtcRegisteredAddressesProvider(
            btcRegistrationConfig.iroha,
            btcRegistrationCredential,
            btcRegistrationCredential.accountId,
            btcRegistrationConfig.notaryAccount
        )
    }

    @Bean
    fun irohaAccountCreator(): IrohaAccountCreator {
        return IrohaAccountCreator(btcClientCreatorConsumer, btcRegistrationConfig.notaryAccount, "bitcoin")
    }
}
