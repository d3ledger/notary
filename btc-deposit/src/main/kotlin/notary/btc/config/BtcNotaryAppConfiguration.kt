package notary.btc.config

import config.loadConfigs
import model.IrohaCredential
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import provider.btc.address.BtcRegisteredAddressesProvider
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil

val notaryConfig = loadConfigs("btc-notary", BtcNotaryConfig::class.java, "/btc/notary.properties")

@Configuration
class BtcNotaryAppConfiguration {

    @Bean
    fun notaryConfig() = notaryConfig

    @Bean
    fun irohaNetwork() = IrohaNetworkImpl(notaryConfig.iroha.hostname, notaryConfig.iroha.port)

    @Bean
    fun btcRegisteredAddressesProvider(): BtcRegisteredAddressesProvider {
        ModelUtil.loadKeypair(notaryConfig.notaryCredential.pubkeyPath, notaryConfig.notaryCredential.privkeyPath)
            .fold({ keypair ->
                return BtcRegisteredAddressesProvider(
                    IrohaCredential(notaryConfig.notaryCredential.accountId, keypair),
                    irohaNetwork(),
                    notaryConfig.registrationAccount,
                    notaryConfig.notaryCredential.accountId
                )
            }, { ex -> throw ex })
    }

    @Bean
    fun notaryCredential(): IrohaCredential {
        //Assuming Iroha library is loaded
        return ModelUtil.loadKeypair(
            notaryConfig.notaryCredential.pubkeyPath,
            notaryConfig.notaryCredential.privkeyPath
        ).fold({ keypair -> IrohaCredential(notaryConfig.notaryCredential.accountId, keypair) }, { ex -> throw ex })
    }
}
