package com.d3.btc.deposit.config

import com.d3.btc.provider.BtcRegisteredAddressesProvider
import config.BitcoinConfig
import config.loadConfigs
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import model.IrohaCredential
import org.bitcoinj.wallet.Wallet
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.util.ModelUtil
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
    fun wallet() = Wallet.loadFromFile(File(depositConfig.bitcoin.walletPath))

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
