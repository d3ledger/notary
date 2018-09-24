package notary.btc.config

import config.loadConfigs
import jp.co.soramitsu.iroha.Keypair
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import provider.btc.BtcRegisteredAddressesProvider
import sidechain.iroha.util.ModelUtil

val notaryConfig = loadConfigs("btc-notary", BtcNotaryConfig::class.java, "/btc/notary.properties")

@Configuration
class BtcNotaryAppConfiguration {

    @Bean
    fun notaryConfig() = notaryConfig

    @Bean
    fun btcRegisteredAddressesProvider(): BtcRegisteredAddressesProvider {
        ModelUtil.loadKeypair(notaryConfig.iroha.pubkeyPath, notaryConfig.iroha.privkeyPath).fold({ keypair ->
            return BtcRegisteredAddressesProvider(
                notaryConfig.iroha,
                keypair,
                notaryConfig.registrationAccount,
                notaryConfig.iroha.creator
            )
        }, { ex -> throw ex })
    }

    @Bean
    fun healthCheckIrohaConfig() = notaryConfig.iroha

    @Bean
    fun healthCheckKeyPair(): Keypair {
        //Assuming Iroha library is loaded
        return ModelUtil.loadKeypair(notaryConfig.iroha.pubkeyPath, notaryConfig.iroha.privkeyPath)
            .fold({ keypair -> keypair }, { ex -> throw ex })
    }
}
