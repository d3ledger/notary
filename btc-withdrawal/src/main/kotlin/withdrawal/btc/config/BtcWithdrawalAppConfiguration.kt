package withdrawal.btc.config

import config.loadConfigs
import model.IrohaCredential
import org.bitcoinj.core.Address
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import provider.btc.address.BtcRegisteredAddressesProvider
import provider.btc.network.BtcNetworkConfigProvider
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import withdrawal.btc.BtcWhiteListProvider
import withdrawal.transaction.SignCollector
import withdrawal.transaction.TransactionSigner

val withdrawalConfig = loadConfigs("btc-withdrawal", BtcWithdrawalConfig::class.java, "/btc/withdrawal.properties")

@Configuration
class BtcWithdrawalAppConfiguration {

    private val withdrawalKeypair = ModelUtil.loadKeypair(
        withdrawalConfig.withdrawalCredential.pubkeyPath,
        withdrawalConfig.withdrawalCredential.privkeyPath
    ).fold({ keypair -> keypair }, { ex -> throw ex })

    @Bean
    fun withdrawalCredential() =
        IrohaCredential(withdrawalConfig.withdrawalCredential.accountId, withdrawalKeypair)

    @Bean
    fun withdrawalConsumer() = IrohaConsumerImpl(withdrawalCredential(), irohaNetwork())

    @Bean
    fun withdrawalConfig() = withdrawalConfig

    @Bean
    fun irohaChainListener() = IrohaChainListener(
        withdrawalConfig.iroha.hostname,
        withdrawalConfig.iroha.port,
        withdrawalCredential()
    )

    @Bean
    fun changeAddress(btcNetworkConfigProvider: BtcNetworkConfigProvider) =
        Address.fromBase58(btcNetworkConfigProvider.getConfig(), withdrawalConfig.changeAddress)

    @Bean
    fun irohaNetwork() = IrohaNetworkImpl(withdrawalConfig.iroha.hostname, withdrawalConfig.iroha.port)

    @Bean
    fun btcRegisteredAddressesProvider(): BtcRegisteredAddressesProvider {
        ModelUtil.loadKeypair(
            withdrawalConfig.notaryCredential.pubkeyPath,
            withdrawalConfig.notaryCredential.privkeyPath
        ).fold({ keypair ->
            return BtcRegisteredAddressesProvider(
                IrohaCredential(withdrawalConfig.notaryCredential.accountId, keypair),
                irohaNetwork(),
                withdrawalConfig.registrationAccount,
                withdrawalConfig.notaryCredential.accountId
            )
        }, { ex -> throw ex })
    }

    @Bean
    fun whiteListProvider() {
        BtcWhiteListProvider(
            withdrawalConfig.registrationAccount,
            withdrawalCredential(),
            irohaNetwork()
        )
    }

    @Bean
    fun transactionSigner() = TransactionSigner(btcRegisteredAddressesProvider())

    @Bean
    fun signCollector() =
        SignCollector(irohaNetwork(), withdrawalCredential(), withdrawalConsumer(), transactionSigner())

}
