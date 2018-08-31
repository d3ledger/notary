@file:JvmName("BtcPkPreGenerationMain")

package registration.btc.key

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.loadConfigs
import mu.KLogging
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.util.ModelUtil

fun main(args: Array<String>) {
    val logger = KLogging()
    val btcPkPreGenConfig =
        loadConfigs("btc-pk-pregen", BtcPkPreGenConfig::class.java, "/btc/pub_key_pregeneration.properties")
    IrohaInitialization.loadIrohaLibrary()
        .flatMap { ModelUtil.loadKeypair(btcPkPreGenConfig.iroha.pubkeyPath, btcPkPreGenConfig.iroha.privkeyPath) }
        .flatMap { keyPair -> BtcPkPreGenInitialization(keyPair, btcPkPreGenConfig).init() }
        .failure { ex ->
            logger.logger.error("cannot run public key pregeneration", ex)
            System.exit(1)
        }
}
