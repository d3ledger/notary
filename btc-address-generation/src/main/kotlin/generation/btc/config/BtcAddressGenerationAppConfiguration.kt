package generation.btc.config

import config.loadConfigs
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
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

val btcAddressGenerationConfig =
    loadConfigs(
        "btc-address-generation",
        BtcAddressGenerationConfig::class.java,
        "/btc/address_generation.properties"
    ).get()

@Configuration
class BtcAddressGenerationAppConfiguration {

    private val registrationKeyPair =
        ModelUtil.loadKeypair(
            btcAddressGenerationConfig.registrationAccount.pubkeyPath,
            btcAddressGenerationConfig.registrationAccount.privkeyPath
        ).fold({ keypair ->
            keypair
        }, { ex -> throw ex })

    private val registrationCredential =
        IrohaCredential(btcAddressGenerationConfig.registrationAccount.accountId, registrationKeyPair)

    private val mstRegistrationKeyPair =
        ModelUtil.loadKeypair(
            btcAddressGenerationConfig.mstRegistrationAccount.pubkeyPath,
            btcAddressGenerationConfig.mstRegistrationAccount.privkeyPath
        ).fold({ keypair ->
            keypair
        }, { ex -> throw ex })

    private val mstRegistrationCredential =
        IrohaCredential(btcAddressGenerationConfig.mstRegistrationAccount.accountId, mstRegistrationKeyPair)

    @Bean
    fun generationIrohaAPI() = IrohaAPI(
        btcAddressGenerationConfig.iroha.hostname,
        btcAddressGenerationConfig.iroha.port
    )

    @Bean
    fun registrationQueryAPI() = QueryAPI(
        generationIrohaAPI(),
        registrationCredential.accountId,
        registrationCredential.keyPair
    )

    @Bean
    fun btcAddressGenerationConfig() = btcAddressGenerationConfig

    @Bean
    fun walletFile(): WalletFile {
        val walletFile = File(btcAddressGenerationConfig.btcWalletFilePath)
        val wallet = Wallet.loadFromFile(walletFile)
        return WalletFile(wallet, walletFile)
    }

    @Bean
    fun notaryPeerListProvider(): NotaryPeerListProvider {
        return NotaryPeerListProviderImpl(
            registrationQueryAPI(),
            btcAddressGenerationConfig.notaryListStorageAccount,
            btcAddressGenerationConfig.notaryListSetterAccount
        )
    }

    @Bean
    fun sessionConsumer() = IrohaConsumerImpl(registrationCredential, generationIrohaAPI())

    @Bean
    fun multiSigConsumer() = IrohaConsumerImpl(mstRegistrationCredential, generationIrohaAPI())

    @Bean
    fun notaryAccount() = btcAddressGenerationConfig.notaryAccount

    @Bean
    fun changeAddressStorageAccount() = btcAddressGenerationConfig.changeAddressesStorageAccount

    @Bean
    fun irohaChainListener() = IrohaChainListener(
        btcAddressGenerationConfig.iroha.hostname,
        btcAddressGenerationConfig.iroha.port,
        registrationCredential
    )

    @Bean
    fun registrationCredential() = registrationCredential
}
