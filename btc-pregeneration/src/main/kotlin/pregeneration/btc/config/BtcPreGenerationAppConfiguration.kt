package pregeneration.btc.config

import config.loadConfigs
import model.IrohaCredential
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import provider.NotaryPeerListProvider
import provider.NotaryPeerListProviderImpl
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.consumer.IrohaNetworkImpl
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

    // TODO add close() on destroy
    @Bean
    fun irohaNetwork() = IrohaNetworkImpl(
        btcPreGenConfig.iroha.hostname,
        btcPreGenConfig.iroha.port
    )

    @Bean
    fun preGenConfig() = btcPreGenConfig

    @Bean
    fun walletFile(): WalletFile {
        val walletFile = File(btcPreGenConfig.btcWalletFilePath)
        val wallet = Wallet.loadFromFile(walletFile)
        return WalletFile(wallet, walletFile)
    }

    @Bean
    @Autowired
    fun notaryPeerListProvider(irohaNetwork: IrohaNetwork): NotaryPeerListProvider {
        return NotaryPeerListProviderImpl(
            registrationCredential,
            irohaNetwork,
            btcPreGenConfig.notaryListStorageAccount,
            btcPreGenConfig.notaryListSetterAccount
        )
    }

    @Bean
    fun btcPublicKeyProviderIrohaConfig() = btcPreGenConfig.iroha

    @Bean
    fun btcRegistrationCredential() = registrationCredential

    @Bean
    fun mstBtcRegistrationCredential() = mstRegistrationCredential

    @Bean
    fun notaryAccount() = btcPreGenConfig.notaryAccount

    @Bean
    // TODO class is Closeable, make sure it is closed
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
