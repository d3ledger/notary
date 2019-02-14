package registration.btc.config

import com.d3.btc.provider.BtcRegisteredAddressesProvider
import com.d3.btc.provider.account.IrohaBtcAccountCreator
import com.d3.btc.provider.address.BtcAddressesProvider
import config.loadConfigs
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import model.IrohaCredential
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import sidechain.iroha.consumer.IrohaConsumerImpl
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
    fun btcAddressesProvider(): BtcAddressesProvider {
        return BtcAddressesProvider(
            queryAPI(),
            btcRegistrationConfig.mstRegistrationAccount,
            btcRegistrationConfig.notaryAccount
        )
    }

    @Bean
    fun btcRegisteredAddressesProvider(): BtcRegisteredAddressesProvider {
        return BtcRegisteredAddressesProvider(
            queryAPI(),
            btcRegistrationCredential.accountId,
            btcRegistrationConfig.notaryAccount
        )
    }

    @Bean
    fun btcRegistrationConsumer() = IrohaConsumerImpl(btcRegistrationCredential, irohaAPI())

    @Bean
    fun irohaBtcAccountCreator(): IrohaBtcAccountCreator {
        return IrohaBtcAccountCreator(
            btcRegistrationConsumer(),
            btcRegistrationConfig.notaryAccount
        )
    }
}
