@file:JvmName("BtcPreGenerationMain")

package registration.btc.pregen

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.loadConfigs
import mu.KLogging
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.util.ModelUtil

private val logger = KLogging().logger
fun main(args: Array<String>) {
    val btcPkPreGenConfig =
        loadConfigs("btc-pregen", BtcPreGenConfig::class.java, "/btc/pregeneration.properties")
    executePreGeneration(btcPkPreGenConfig)
}

fun executePreGeneration(btcPkPreGenConfig: BtcPreGenConfig) {
    logger.info { "Run BTC multisignature address pregeneration" }
    IrohaInitialization.loadIrohaLibrary()
        .flatMap { ModelUtil.loadKeypair(btcPkPreGenConfig.iroha.pubkeyPath, btcPkPreGenConfig.iroha.privkeyPath) }
        .flatMap { keyPair -> BtcPreGenInitialization(keyPair, btcPkPreGenConfig).init() }
        .failure { ex ->
            logger.error("cannot run btc address pregeneration", ex)
            System.exit(1)
        }
}
