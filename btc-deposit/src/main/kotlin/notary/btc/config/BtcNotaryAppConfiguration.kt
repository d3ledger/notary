package notary.btc.config

import com.d3.btc.provider.BtcRegisteredAddressesProvider
import config.BitcoinConfig
import config.loadConfigs
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import model.IrohaCredential
import org.bitcoinj.wallet.Wallet
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.util.ModelUtil
import java.io.File

val notaryConfig = loadConfigs("btc-notary", BtcNotaryConfig::class.java, "/btc/notary.properties").get()

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
    fun irohaAPI() = IrohaAPI(notaryConfig.iroha.hostname, notaryConfig.iroha.port)

    @Bean
    fun keypair() =
        ModelUtil.loadKeypair(notaryConfig.notaryCredential.pubkeyPath, notaryConfig.notaryCredential.privkeyPath)


    @Bean
    fun btcRegisteredAddressesProvider(): BtcRegisteredAddressesProvider {
        ModelUtil.loadKeypair(notaryConfig.notaryCredential.pubkeyPath, notaryConfig.notaryCredential.privkeyPath)
            .fold({ keypair ->
                return BtcRegisteredAddressesProvider(
                    QueryAPI(irohaAPI(), notaryConfig.notaryCredential.accountId, keypair),
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
    fun depositIrohaChainListener() = IrohaChainListener(
        notaryConfig.iroha.hostname,
        notaryConfig.iroha.port,
        notaryCredential
    )

    @Bean
    fun blockStoragePath() = notaryConfig().bitcoin.blockStoragePath

    @Bean
    fun btcHosts() = BitcoinConfig.extractHosts(notaryConfig().bitcoin)
}
