@file:JvmName("BtcNotaryMain")

package notary.btc

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.loadConfigs
import mu.KLogging
import provider.btc.BtcTakenAddressesProvider
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.util.ModelUtil

private val logger = KLogging().logger

fun main(args: Array<String>) {
    val notaryConfig = loadConfigs("btc-notary", BtcNotaryConfig::class.java, "/btc/notary.properties")
    executeNotary(notaryConfig)
}

fun executeNotary(notaryConfig: BtcNotaryConfig) {
    logger.info { "Run BTC notary" }
    IrohaInitialization.loadIrohaLibrary()
        .flatMap { ModelUtil.loadKeypair(notaryConfig.iroha.pubkeyPath, notaryConfig.iroha.privkeyPath) }
        .flatMap { keypair ->
            val btcTakenAddressesProvider = BtcTakenAddressesProvider(
                notaryConfig.iroha,
                keypair,
                notaryConfig.registrationAccount,
                notaryConfig.iroha.creator
            )
            BtcNotaryInitialization(notaryConfig, btcTakenAddressesProvider).init()
        }
        .failure { ex ->
            logger.error("Cannot run btc notary", ex)
            System.exit(1)
        }
}
