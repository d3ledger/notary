@file:JvmName("BtcNotaryMain")

package notary.btc

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.loadConfigs
import mu.KLogging
import notary.NotaryConfig
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.util.ModelUtil

fun main(args: Array<String>) {
    val logger = KLogging()
    val notaryConfig = loadConfigs("notary", NotaryConfig::class.java)
    IrohaInitialization.loadIrohaLibrary()
        .flatMap { ModelUtil.loadKeypair(notaryConfig.iroha.pubkeyPath, notaryConfig.iroha.privkeyPath) }
        .flatMap { keypair ->
            BtcNotaryInitialization(notaryConfig).init()
        }
        .failure { ex ->
            logger.logger.error { ex }
            System.exit(1)
        }
}
