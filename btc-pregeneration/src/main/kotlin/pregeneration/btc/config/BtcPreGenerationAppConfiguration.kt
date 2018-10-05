package pregeneration.btc.config

import config.loadConfigs
import model.IrohaCredential
import org.bitcoinj.wallet.Wallet
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import provider.NotaryPeerListProviderImpl
import provider.btc.BtcPublicKeyProvider
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.util.ModelUtil
import java.io.File

val btcPreGenConfig =
    loadConfigs("btc-pregen", BtcPreGenConfig::class.java, "/pregeneration.properties")

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
    fun btcPublicKeyProvider(): BtcPublicKeyProvider {
        val walletFile = File(btcPreGenConfig.btcWalletFilePath)
        val wallet = Wallet.loadFromFile(walletFile)
        val notaryPeerListProvider = NotaryPeerListProviderImpl(
            btcPreGenConfig.iroha,
            registrationCredential,
            btcPreGenConfig.notaryListStorageAccount,
            btcPreGenConfig.notaryListSetterAccount
        )
        return BtcPublicKeyProvider(
            wallet,
            walletFile,
            btcPreGenConfig.iroha,
            notaryPeerListProvider,
            registrationCredential,
            mstRegistrationCredential,
            btcPreGenConfig.notaryAccount
        )
    }

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
