package notary.btc.config

import config.loadConfigs
import model.IrohaCredential
import org.bitcoinj.wallet.Wallet
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import provider.btc.address.BtcRegisteredAddressesProvider
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import java.io.File

val notaryConfig = loadConfigs("btc-notary", BtcNotaryConfig::class.java, "/btc/notary.properties")

@Configuration
class BtcNotaryAppConfiguration {

    private val notaryKeypair = ModelUtil.loadKeypair(
        notaryConfig.notaryCredential.pubkeyPath,
        notaryConfig.notaryCredential.privkeyPath
    ).fold({ keypair -> keypair }, { ex -> throw ex })

    private val notaryCredential = IrohaCredential(notaryConfig.notaryCredential.accountId, notaryKeypair)

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
    fun wallet() = Wallet.loadFromFile(File(notaryConfig.bitcoin.walletPath))

    @Bean
    fun notaryCredential() = notaryCredential

    @Bean
    fun irohaChainListener() = IrohaChainListener(
        notaryConfig.iroha.hostname,
        notaryConfig.iroha.port,
        notaryCredential
    )
}
