package notary.btc.config

import config.loadConfigs
import jp.co.soramitsu.iroha.Keypair
import model.IrohaCredential
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import provider.btc.BtcRegisteredAddressesProvider
import sidechain.iroha.util.ModelUtil

val notaryConfig = loadConfigs("btc-notary", BtcNotaryConfig::class.java, "/notary.properties")

@Configuration
class BtcNotaryAppConfiguration {

    @Bean
    fun notaryConfig() = notaryConfig

    @Bean
    fun btcRegisteredAddressesProvider(): BtcRegisteredAddressesProvider {
        ModelUtil.loadKeypair(notaryConfig.notaryCredential.pubkeyPath, notaryConfig.notaryCredential.privkeyPath)
            .fold({ keypair ->
                return BtcRegisteredAddressesProvider(
                    notaryConfig.iroha,
                    IrohaCredential(notaryConfig.notaryCredential.accountId, keypair),
                    notaryConfig.registrationAccount,
                    notaryConfig.notaryCredential.accountId
                )
            }, { ex -> throw ex })
    }

    @Bean
    fun healthCheckIrohaConfig() = notaryConfig.iroha

    @Bean
    fun healthCheckCredential(): IrohaCredential {
        //Assuming Iroha library is loaded

        return ModelUtil.loadKeypair(
            notaryConfig.notaryCredential.pubkeyPath,
            notaryConfig.notaryCredential.privkeyPath
        )
            .fold({ keypair -> IrohaCredential(notaryConfig.notaryCredential.accountId, keypair) }, { ex -> throw ex })
    }
}
