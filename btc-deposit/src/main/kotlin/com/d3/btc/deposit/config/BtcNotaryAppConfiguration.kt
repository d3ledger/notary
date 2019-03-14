package com.d3.btc.deposit.config

import com.d3.btc.deposit.BTC_DEPOSIT_SERVICE_NAME
import com.d3.btc.provider.BtcRegisteredAddressesProvider
import com.d3.commons.config.BitcoinConfig
import com.d3.commons.config.loadConfigs
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.IrohaChainListener
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.createPrettySingleThreadPool
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import org.bitcoinj.wallet.Wallet
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

val depositConfig = loadConfigs("btc-deposit", BtcDepositConfig::class.java, "/btc/deposit.properties").get()

@Configuration
class BtcNotaryAppConfiguration {

    private val notaryKeypair = ModelUtil.loadKeypair(
        depositConfig.notaryCredential.pubkeyPath,
        depositConfig.notaryCredential.privkeyPath
    ).fold({ keypair -> keypair }, { ex -> throw ex })

    private val notaryCredential = IrohaCredential(depositConfig.notaryCredential.accountId, notaryKeypair)

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
                    QueryAPI(irohaAPI(), depositConfig.notaryCredential.accountId, keypair),
                    depositConfig.registrationAccount,
                    depositConfig.notaryCredential.accountId
                )
            }, { ex -> throw ex })
    }

    @Bean
    fun registeredClientsListenerExecutor() =
        createPrettySingleThreadPool(BTC_DEPOSIT_SERVICE_NAME, "reg-clients-listener")

    @Bean
    fun transferWallet() = Wallet.loadFromFile(File(depositConfig.btcTransferWalletPath))

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
}
