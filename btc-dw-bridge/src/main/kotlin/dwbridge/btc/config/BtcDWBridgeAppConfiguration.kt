package dwbridge.btc.config

import config.BitcoinConfig
import config.loadConfigs
import fee.BtcFeeRateService
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import model.IrohaCredential
import notary.btc.config.BtcNotaryConfig
import org.bitcoinj.wallet.Wallet
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import provider.btc.address.BtcRegisteredAddressesProvider
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil
import withdrawal.btc.config.BtcWithdrawalConfig
import withdrawal.btc.provider.BtcChangeAddressProvider
import withdrawal.btc.provider.BtcWhiteListProvider
import withdrawal.btc.statistics.WithdrawalStatistics
import java.io.File

val withdrawalConfig =
    loadConfigs("btc-withdrawal", BtcWithdrawalConfig::class.java, "/btc/withdrawal.properties").get()
val notaryConfig = loadConfigs("btc-notary", BtcNotaryConfig::class.java, "/btc/notary.properties").get()
val dwBridgeConfig = loadConfigs("btc-dw-bridge", BtcDWBridgeConfig::class.java, "/btc/dw-bridge.properties").get()

@Configuration
class BtcDWBridgeAppConfiguration {

    private val withdrawalKeypair = ModelUtil.loadKeypair(
        withdrawalConfig.withdrawalCredential.pubkeyPath,
        withdrawalConfig.withdrawalCredential.privkeyPath
    ).fold({ keypair -> keypair }, { ex -> throw ex })

    private val notaryKeypair = ModelUtil.loadKeypair(
        notaryConfig.notaryCredential.pubkeyPath,
        notaryConfig.notaryCredential.privkeyPath
    ).fold({ keypair -> keypair }, { ex -> throw ex })

    private val signatureCollectorKeypair = ModelUtil.loadKeypair(
        withdrawalConfig.signatureCollectorCredential.pubkeyPath,
        withdrawalConfig.signatureCollectorCredential.privkeyPath
    ).fold({ keypair -> keypair }, { ex -> throw ex })

    private val btcFeeRateCredential = ModelUtil.loadKeypair(
        withdrawalConfig.btcFeeRateCredential.pubkeyPath,
        withdrawalConfig.btcFeeRateCredential.privkeyPath
    ).fold({ keypair ->
        IrohaCredential(withdrawalConfig.btcFeeRateCredential.accountId, keypair)
    }, { ex -> throw ex })

    private val notaryCredential =
        IrohaCredential(notaryConfig.notaryCredential.accountId, notaryKeypair)

    @Bean
    fun notaryConfig() = notaryConfig

    @Bean
    fun irohaAPI() = IrohaAPI(dwBridgeConfig.iroha.hostname, dwBridgeConfig.iroha.port)

    @Bean
    fun btcRegisteredAddressesProvider(): BtcRegisteredAddressesProvider {
        ModelUtil.loadKeypair(
            notaryConfig.notaryCredential.pubkeyPath,
            notaryConfig.notaryCredential.privkeyPath
        )
            .fold({ keypair ->
                return BtcRegisteredAddressesProvider(
                    QueryAPI(irohaAPI(), notaryConfig.notaryCredential.accountId, keypair),
                    notaryConfig.registrationAccount,
                    notaryConfig.notaryCredential.accountId
                )
            }, { ex -> throw ex })
    }

    @Bean
    fun signatureCollectorCredential() =
        IrohaCredential(withdrawalConfig.signatureCollectorCredential.accountId, signatureCollectorKeypair)

    @Bean
    fun signatureCollectorConsumer() = IrohaConsumerImpl(signatureCollectorCredential(), irohaAPI())

    @Bean
    fun wallet() = Wallet.loadFromFile(File(dwBridgeConfig.bitcoin.walletPath))

    @Bean
    fun notaryCredential() = notaryCredential

    @Bean
    fun withdrawalStatistics() = WithdrawalStatistics.create()

    @Bean
    fun withdrawalCredential() =
        IrohaCredential(withdrawalConfig.withdrawalCredential.accountId, withdrawalKeypair)

    @Bean
    fun withdrawalConsumer() = IrohaConsumerImpl(withdrawalCredential(), irohaAPI())

    @Bean
    fun withdrawalConfig() = withdrawalConfig

    @Bean
    fun depositIrohaChainListener() = IrohaChainListener(
        dwBridgeConfig.iroha.hostname,
        dwBridgeConfig.iroha.port,
        notaryCredential()
    )

    @Bean
    fun withdrawalIrohaChainListener() = IrohaChainListener(
        dwBridgeConfig.iroha.hostname,
        dwBridgeConfig.iroha.port,
        withdrawalCredential()
    )

    @Bean
    fun withdrawalQueryAPI() = QueryAPI(irohaAPI(), withdrawalCredential().accountId, withdrawalCredential().keyPair)

    @Bean
    fun whiteListProvider(): BtcWhiteListProvider {
        return BtcWhiteListProvider(
            withdrawalConfig.registrationCredential.accountId,
            withdrawalQueryAPI()
        )
    }

    @Bean
    fun btcChangeAddressProvider(): BtcChangeAddressProvider {
        return BtcChangeAddressProvider(
            withdrawalQueryAPI(),
            withdrawalConfig.mstRegistrationAccount,
            withdrawalConfig.changeAddressesStorageAccount
        )
    }

    @Bean
    fun blockStoragePath() = dwBridgeConfig.bitcoin.blockStoragePath

    @Bean
    fun btcHosts() = BitcoinConfig.extractHosts(dwBridgeConfig.bitcoin)

    @Bean
    fun btcFeeRateAccount() = withdrawalConfig.btcFeeRateCredential.accountId

    @Bean
    fun btcFeeRateService() =
        BtcFeeRateService(
            IrohaConsumerImpl(
                btcFeeRateCredential, irohaAPI()
            ),
            btcFeeRateCredential.accountId,
            QueryAPI(irohaAPI(), btcFeeRateCredential.accountId, btcFeeRateCredential.keyPair)
        )

}
