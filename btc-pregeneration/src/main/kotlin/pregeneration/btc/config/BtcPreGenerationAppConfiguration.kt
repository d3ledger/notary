package pregeneration.btc.config

import config.loadConfigs
import model.IrohaCredential
import org.bitcoinj.wallet.Wallet
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import provider.NotaryPeerListProvider
import provider.NotaryPeerListProviderImpl
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil
import wallet.WalletFile
import java.io.File

val btcPreGenConfig =
    loadConfigs("btc-pregen", BtcPreGenConfig::class.java, "/btc/pregeneration.properties")

@Configuration
class BtcPreGenerationAppConfiguration {

    private val registrationKeyPair =
        ModelUtil.loadKeypair(
            btcPreGenConfig.registrationAccount.pubkeyPath,
            btcPreGenConfig.registrationAccount.privkeyPath
        ).fold({ keypair ->
            keypair
        }, { ex -> throw ex })

    private val registrationCredential =
        IrohaCredential(btcPreGenConfig.registrationAccount.accountId, registrationKeyPair)

    private val mstRegistrationKeyPair =
        ModelUtil.loadKeypair(
            btcPreGenConfig.mstRegistrationAccount.pubkeyPath,
            btcPreGenConfig.mstRegistrationAccount.privkeyPath
        ).fold({ keypair ->
            keypair
        }, { ex -> throw ex })

    private val mstRegistrationCredential =
        IrohaCredential(btcPreGenConfig.mstRegistrationAccount.accountId, mstRegistrationKeyPair)

    @Bean
    fun preGenConfig() = btcPreGenConfig

    @Bean
    fun walletFile(): WalletFile {
        val walletFile = File(btcPreGenConfig.btcWalletFilePath)
        val wallet = Wallet.loadFromFile(walletFile)
        return WalletFile(wallet, walletFile);
    }

    @Bean
    fun notaryPeerListProvider(): NotaryPeerListProvider {
        return NotaryPeerListProviderImpl(
            btcPreGenConfig.iroha,
            registrationCredential,
            btcPreGenConfig.notaryListStorageAccount,
            btcPreGenConfig.notaryListSetterAccount
        )
    }

    @Bean
    fun sessionConsumer() = IrohaConsumerImpl(registrationCredential, btcPreGenConfig.iroha)

    @Bean
    fun multiSigConsumer() = IrohaConsumerImpl(mstRegistrationCredential, btcPreGenConfig.iroha)

    @Bean
    fun notaryAccount() = btcPreGenConfig.notaryAccount

    @Bean
    fun irohaChainListener() = IrohaChainListener(
        btcPreGenConfig.iroha.hostname,
        btcPreGenConfig.iroha.port,
        registrationCredential
    )

    @Bean
    fun healthCheckIrohaConfig() = btcPreGenConfig.iroha

    //TODO dedicate a special account for performing Iroha health checks
    @Bean
    fun irohaHealthCheckCredential() = registrationCredential

    @Bean
    fun registrationCredential() = registrationCredential
}
