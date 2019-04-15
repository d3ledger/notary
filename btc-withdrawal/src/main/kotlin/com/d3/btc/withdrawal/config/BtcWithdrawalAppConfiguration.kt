package com.d3.btc.withdrawal.config

import com.d3.btc.provider.BtcRegisteredAddressesProvider
import com.d3.btc.withdrawal.BTC_WITHDRAWAL_SERVICE_NAME
import com.d3.btc.provider.BtcChangeAddressProvider
import com.d3.btc.withdrawal.provider.BtcWhiteListProvider
import com.d3.btc.withdrawal.statistics.WithdrawalStatistics
import com.d3.commons.config.*
import com.d3.commons.model.IrohaCredential
import com.d3.commons.provider.NotaryPeerListProviderImpl
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.createPrettySingleThreadPool
import io.grpc.ManagedChannelBuilder
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import org.bitcoinj.wallet.Wallet
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

val withdrawalConfig =
    loadConfigs("btc-withdrawal", BtcWithdrawalConfig::class.java, "/btc/withdrawal.properties").get()

//TODO refactor config
@Configuration
class BtcWithdrawalAppConfiguration {

    private val rmqConfig = loadRawConfigs("rmq", RMQConfig::class.java, "${getConfigFolder()}/rmq.properties")

    private val withdrawalKeypair = ModelUtil.loadKeypair(
        withdrawalConfig.withdrawalCredential.pubkeyPath,
        withdrawalConfig.withdrawalCredential.privkeyPath
    ).fold({ keypair -> keypair }, { ex -> throw ex })

    private val signatureCollectorKeypair = ModelUtil.loadKeypair(
        withdrawalConfig.signatureCollectorCredential.pubkeyPath,
        withdrawalConfig.signatureCollectorCredential.privkeyPath
    ).fold({ keypair -> keypair }, { ex -> throw ex })

    private val btcRegistrationCredential = ModelUtil.loadKeypair(
        withdrawalConfig.registrationCredential.pubkeyPath,
        withdrawalConfig.registrationCredential.privkeyPath
    ).fold({ keypair ->
        IrohaCredential(withdrawalConfig.registrationCredential.accountId, keypair)
    }, { ex -> throw ex })

    private val btcConsensusCredential = ModelUtil.loadKeypair(
        withdrawalConfig.btcConsensusCredential.pubkeyPath,
        withdrawalConfig.btcConsensusCredential.privkeyPath
    ).fold({ keypair ->
        IrohaCredential(withdrawalConfig.btcConsensusCredential.accountId, keypair)
    }, { ex -> throw ex })

    @Bean
    fun rmqConfig() = rmqConfig

    @Bean
    fun consensusIrohaCredential() = btcConsensusCredential

    @Bean
    fun consensusIrohaConsumer() = IrohaConsumerImpl(consensusIrohaCredential(), irohaAPI())

    @Bean
    fun healthCheckPort() = withdrawalConfig.healthCheckPort

    @Bean
    fun withdrawalStatistics() = WithdrawalStatistics.create()

    @Bean
    fun transferWallet() = Wallet.loadFromFile(File(withdrawalConfig.btcTransfersWalletPath))

    @Bean
    fun withdrawalCredential() =
        IrohaCredential(withdrawalConfig.withdrawalCredential.accountId, withdrawalKeypair)

    @Bean
    fun withdrawalConsumer() = IrohaConsumerImpl(withdrawalCredential(), irohaAPI())

    @Bean
    fun signatureCollectorCredential() =
        IrohaCredential(withdrawalConfig.signatureCollectorCredential.accountId, signatureCollectorKeypair)

    @Bean
    fun signatureCollectorConsumer() = IrohaConsumerImpl(signatureCollectorCredential(), irohaAPI())

    @Bean
    fun withdrawalConfig() = withdrawalConfig

    @Bean
    fun irohaAPI(): IrohaAPI {
        val irohaAPI = IrohaAPI(withdrawalConfig.iroha.hostname, withdrawalConfig.iroha.port)
        /**
         * It's essential to handle blocks in this service one-by-one.
         * This is why we explicitly set single threaded executor.
         */
        irohaAPI.setChannelForStreamingQueryStub(
            ManagedChannelBuilder.forAddress(
                withdrawalConfig.iroha.hostname,
                withdrawalConfig.iroha.port
            ).executor(
                createPrettySingleThreadPool(
                    BTC_WITHDRAWAL_SERVICE_NAME,
                    "iroha-chain-listener"
                )
            ).usePlaintext().build()
        )
        return irohaAPI
    }

    @Bean
    fun btcRegisteredAddressesProvider(): BtcRegisteredAddressesProvider {
        return BtcRegisteredAddressesProvider(
            QueryAPI(irohaAPI(), btcRegistrationCredential.accountId, btcRegistrationCredential.keyPair),
            withdrawalConfig.registrationCredential.accountId,
            withdrawalConfig.notaryCredential.accountId
        )
    }

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
    fun blockStoragePath() = withdrawalConfig().bitcoin.blockStoragePath

    @Bean
    fun btcHosts() = BitcoinConfig.extractHosts(withdrawalConfig().bitcoin)

    @Bean
    fun notaryPeerListProvider() =
        NotaryPeerListProviderImpl(
            withdrawalQueryAPI(),
            withdrawalConfig.notaryListStorageAccount,
            withdrawalConfig.notaryListSetterAccount
        )
}
