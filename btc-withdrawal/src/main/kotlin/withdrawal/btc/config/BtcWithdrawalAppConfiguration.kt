package withdrawal.btc.config

import config.loadConfigs
import model.IrohaCredential
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import provider.btc.address.BtcRegisteredAddressesProvider
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import withdrawal.btc.provider.BtcChangeAddressProvider
import withdrawal.btc.provider.BtcWhiteListProvider

val withdrawalConfig = loadConfigs("btc-withdrawal", BtcWithdrawalConfig::class.java, "/btc/withdrawal.properties")

@Configuration
class BtcWithdrawalAppConfiguration {

    private val withdrawalKeypair = ModelUtil.loadKeypair(
        withdrawalConfig.withdrawalCredential.pubkeyPath,
        withdrawalConfig.withdrawalCredential.privkeyPath
    ).fold({ keypair -> keypair }, { ex -> throw ex })

    private val btcRegistrationCredential = ModelUtil.loadKeypair(
        withdrawalConfig.registrationCredential.pubkeyPath,
        withdrawalConfig.registrationCredential.privkeyPath
    ).fold({ keypair ->
        IrohaCredential(withdrawalConfig.registrationCredential.accountId, keypair)
    }, { ex -> throw ex })

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
    fun irohaNetwork() = IrohaNetworkImpl(withdrawalConfig.iroha.hostname, withdrawalConfig.iroha.port)

    @Bean
    fun btcRegisteredAddressesProvider(): BtcRegisteredAddressesProvider {
        return BtcRegisteredAddressesProvider(
            btcRegistrationCredential,
            irohaNetwork(),
            withdrawalConfig.registrationCredential.accountId,
            withdrawalConfig.notaryCredential.accountId
        )
    }

    @Bean
    fun whiteListProvider(): BtcWhiteListProvider {
        return BtcWhiteListProvider(
            withdrawalConfig.registrationCredential.accountId,
            withdrawalCredential(),
            irohaNetwork()
        )
    }

    @Bean
    fun btcChangeAddressProvider(): BtcChangeAddressProvider {
        return BtcChangeAddressProvider(
            withdrawalCredential(),
            irohaNetwork(),
            withdrawalConfig.mstRegistrationAccount,
            withdrawalConfig.changeAddressesStorageAccount
        )
    }
}
