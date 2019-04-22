package com.d3.btc.deposit.config

import com.d3.btc.deposit.BTC_DEPOSIT_SERVICE_NAME
import com.d3.btc.provider.BtcChangeAddressProvider
import com.d3.btc.provider.BtcRegisteredAddressesProvider
import com.d3.btc.wallet.WalletInitializer
import com.d3.btc.wallet.loadAutoSaveWallet
import com.d3.commons.config.BitcoinConfig
import com.d3.commons.config.loadConfigs
import com.d3.commons.model.IrohaCredential
import com.d3.commons.notary.NotaryImpl
import com.d3.commons.sidechain.SideChainEvent
import com.d3.commons.sidechain.iroha.IrohaChainListener
import com.d3.commons.sidechain.iroha.consumer.MultiSigIrohaConsumer
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.createPrettySingleThreadPool
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

val depositConfig = loadConfigs("btc-deposit", BtcDepositConfig::class.java, "/btc/deposit.properties").get()

@Configuration
class BtcNotaryAppConfiguration {

    private val notaryKeypair = ModelUtil.loadKeypair(
        depositConfig.notaryCredential.pubkeyPath,
        depositConfig.notaryCredential.privkeyPath
    ).fold({ keypair -> keypair }, { ex -> throw ex })

    private val notaryCredential = IrohaCredential(depositConfig.notaryCredential.accountId, notaryKeypair)

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
    fun notary() = NotaryImpl(MultiSigIrohaConsumer(notaryCredential, irohaAPI()), btcEventsObservable())

    @Bean
    fun notaryConfig() = depositConfig

    @Bean
    fun healthCheckPort() = depositConfig.healthCheckPort

    @Bean
    fun irohaAPI() = IrohaAPI(depositConfig.iroha.hostname, depositConfig.iroha.port)

    @Bean
    fun keypair() =
        ModelUtil.loadKeypair(depositConfig.notaryCredential.pubkeyPath, depositConfig.notaryCredential.privkeyPath)


    @Bean
    fun btcRegisteredAddressesProvider(): BtcRegisteredAddressesProvider {
        ModelUtil.loadKeypair(depositConfig.notaryCredential.pubkeyPath, depositConfig.notaryCredential.privkeyPath)
            .fold({ keypair ->
                return BtcRegisteredAddressesProvider(
                    queryAPI(),
                    depositConfig.registrationAccount,
                    depositConfig.notaryCredential.accountId
                )
            }, { ex -> throw ex })
    }

    @Bean
    fun registeredClientsListenerExecutor() =
        createPrettySingleThreadPool(BTC_DEPOSIT_SERVICE_NAME, "reg-clients-listener")

    @Bean
    fun transferWallet() = loadAutoSaveWallet(depositConfig.btcTransferWalletPath)

    @Bean
    fun notaryCredential() = notaryCredential

    @Bean
    fun depositIrohaChainListener() = IrohaChainListener(
        depositConfig.iroha.hostname,
        depositConfig.iroha.port,
        notaryCredential
    )

    @Bean
    fun blockStoragePath() = notaryConfig().bitcoin.blockStoragePath

    @Bean
    fun btcHosts() = BitcoinConfig.extractHosts(notaryConfig().bitcoin)

    @Bean
    fun btcChangeAddressProvider() = BtcChangeAddressProvider(
        queryAPI(),
        depositConfig.mstRegistrationAccount,
        depositConfig.changeAddressesStorageAccount
    )

    @Bean
    fun walletInitializer() = WalletInitializer(btcRegisteredAddressesProvider(), btcChangeAddressProvider())
}
