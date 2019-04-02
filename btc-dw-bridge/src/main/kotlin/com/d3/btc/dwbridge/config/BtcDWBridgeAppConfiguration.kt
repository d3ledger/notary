/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.btc.dwbridge.config

import com.d3.btc.deposit.BTC_DEPOSIT_SERVICE_NAME
import com.d3.btc.deposit.config.BtcDepositConfig
import com.d3.btc.dwbridge.BTC_DW_BRIDGE_SERVICE_NAME
import com.d3.btc.fee.BtcFeeRateService
import com.d3.btc.provider.BtcRegisteredAddressesProvider
import com.d3.btc.withdrawal.config.BtcWithdrawalConfig
import com.d3.btc.withdrawal.provider.BtcChangeAddressProvider
import com.d3.btc.withdrawal.provider.BtcWhiteListProvider
import com.d3.btc.withdrawal.statistics.WithdrawalStatistics
import com.d3.commons.config.*
import com.d3.commons.model.IrohaCredential
import com.d3.commons.notary.NotaryImpl
import com.d3.commons.provider.NotaryPeerListProviderImpl
import com.d3.commons.sidechain.SideChainEvent
import com.d3.commons.sidechain.iroha.IrohaChainListener
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.createPrettySingleThreadPool
import io.grpc.ManagedChannelBuilder
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import org.bitcoinj.wallet.Wallet
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

val withdrawalConfig =
    loadConfigs("btc-withdrawal", BtcWithdrawalConfig::class.java, "/btc/withdrawal.properties").get()
val depositConfig = loadConfigs("btc-deposit", BtcDepositConfig::class.java, "/btc/deposit.properties").get()
val dwBridgeConfig = loadConfigs("btc-dw-bridge", BtcDWBridgeConfig::class.java, "/btc/dw-bridge.properties").get()

@Configuration
class BtcDWBridgeAppConfiguration {

    private val rmqConfig = loadRawConfigs("rmq", RMQConfig::class.java, "${getConfigFolder()}/rmq.properties")

    private val withdrawalKeypair = ModelUtil.loadKeypair(
        withdrawalConfig.withdrawalCredential.pubkeyPath,
        withdrawalConfig.withdrawalCredential.privkeyPath
    ).fold({ keypair -> keypair }, { ex -> throw ex })

    private val notaryKeypair = ModelUtil.loadKeypair(
        depositConfig.notaryCredential.pubkeyPath,
        depositConfig.notaryCredential.privkeyPath
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


    private val btcConsensusCredential = ModelUtil.loadKeypair(
        withdrawalConfig.btcConsensusCredential.pubkeyPath,
        withdrawalConfig.btcConsensusCredential.privkeyPath
    ).fold({ keypair ->
        IrohaCredential(withdrawalConfig.btcConsensusCredential.accountId, keypair)
    }, { ex -> throw ex })

    private val notaryCredential =
        IrohaCredential(depositConfig.notaryCredential.accountId, notaryKeypair)

    @Bean
    fun consensusIrohaCredential() = btcConsensusCredential

    @Bean
    fun consensusIrohaConsumer() = IrohaConsumerImpl(consensusIrohaCredential(), irohaAPI())

    @Bean
    fun confidenceListenerExecutorService() =
        createPrettySingleThreadPool(BTC_DEPOSIT_SERVICE_NAME, "tx-confidence-listener")

    @Bean
    fun queryAPI() = QueryAPI(irohaAPI(), notaryCredential.accountId, notaryKeypair)

    @Bean
    fun btcEventsSource(): PublishSubject<SideChainEvent.PrimaryBlockChainEvent> {
        return PublishSubject.create<SideChainEvent.PrimaryBlockChainEvent>()
    }

    @Bean
    fun btcEventsObservable(): Observable<SideChainEvent.PrimaryBlockChainEvent> {
        return btcEventsSource()
    }

    @Bean
    fun notary() = NotaryImpl(notaryCredential, irohaAPI(), btcEventsObservable())

    @Bean
    fun rmqConfig() = rmqConfig

    @Bean
    fun irohaBlocksQueue() = withdrawalConfig.irohaBlockQueue

    @Bean
    fun notaryConfig() = depositConfig

    @Bean
    fun healthCheckPort() = dwBridgeConfig.healthCheckPort

    @Bean
    fun irohaAPI(): IrohaAPI {
        val irohaAPI = IrohaAPI(dwBridgeConfig.iroha.hostname, dwBridgeConfig.iroha.port)
        /**
         * It's essential to handle blocks in this service one-by-one.
         * This is why we explicitly set single threaded executor.
         */
        irohaAPI.setChannelForStreamingQueryStub(
            ManagedChannelBuilder.forAddress(
                dwBridgeConfig.iroha.hostname,
                dwBridgeConfig.iroha.port
            ).executor(
                createPrettySingleThreadPool(
                    BTC_DW_BRIDGE_SERVICE_NAME,
                    "iroha-chain-listener"
                )
            ).usePlaintext().build()
        )
        return irohaAPI
    }

    @Bean
    fun registeredClientsListenerExecutor() =
        createPrettySingleThreadPool(BTC_DW_BRIDGE_SERVICE_NAME, "reg-clients-listener")

    @Bean
    fun btcRegisteredAddressesProvider(): BtcRegisteredAddressesProvider {
        ModelUtil.loadKeypair(
            depositConfig.notaryCredential.pubkeyPath,
            depositConfig.notaryCredential.privkeyPath
        )
            .fold({ keypair ->
                return BtcRegisteredAddressesProvider(
                    QueryAPI(irohaAPI(), depositConfig.notaryCredential.accountId, keypair),
                    depositConfig.registrationAccount,
                    depositConfig.notaryCredential.accountId
                )
            }, { ex -> throw ex })
    }

    @Bean
    fun signatureCollectorCredential() =
        IrohaCredential(withdrawalConfig.signatureCollectorCredential.accountId, signatureCollectorKeypair)

    @Bean
    fun signatureCollectorConsumer() = IrohaConsumerImpl(signatureCollectorCredential(), irohaAPI())

    @Bean
    fun transferWallet() = Wallet.loadFromFile(File(depositConfig.btcTransferWalletPath))

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

    @Bean
    fun notaryPeerListProvider() =
        NotaryPeerListProviderImpl(
            withdrawalQueryAPI(),
            withdrawalConfig.notaryListStorageAccount,
            withdrawalConfig.notaryListSetterAccount
        )
}
