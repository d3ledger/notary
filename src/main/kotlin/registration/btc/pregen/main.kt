@file:JvmName("BtcPreGenerationMain")

package registration.btc.pregen

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.loadConfigs
import mu.KLogging
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.util.ModelUtil

fun main(args: Array<String>) {
    val btcPkPreGenConfig =
        loadConfigs("btc-pregen", BtcPreGenConfig::class.java, "/btc/pregeneration.properties")
    executePreGeneration(btcPkPreGenConfig)
}

fun executePreGeneration(btcPkPreGenConfig: BtcPreGenConfig) {
    val logger = KLogging()
    IrohaInitialization.loadIrohaLibrary()
        .flatMap { ModelUtil.loadKeypair(btcPkPreGenConfig.iroha.pubkeyPath, btcPkPreGenConfig.iroha.privkeyPath) }
        .flatMap { keyPair -> BtcPreGenInitialization(keyPair, btcPkPreGenConfig).init() }
        .failure { ex ->
            logger.logger.error("cannot run btc address pre generation", ex)
            System.exit(1)
        }
}
