@file:JvmName("BtcNotaryMain")

package notary.btc

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.loadConfigs
import mu.KLogging
import provider.btc.BtcAddressesProvider
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.util.ModelUtil

fun main(args: Array<String>) {
    val notaryConfig = loadConfigs("btc-notary", BtcNotaryConfig::class.java, "/btc/notary.properties")
    executeNotary(notaryConfig)
}

fun executeNotary(notaryConfig: BtcNotaryConfig) {
    val logger = KLogging()
    IrohaInitialization.loadIrohaLibrary()
        .flatMap { ModelUtil.loadKeypair(notaryConfig.iroha.pubkeyPath, notaryConfig.iroha.privkeyPath) }
        .flatMap { keypair ->
            val btcAddressesProvider = BtcAddressesProvider(
                notaryConfig.iroha,
                keypair,
                notaryConfig.notaryIrohaAccount
            )
            BtcNotaryInitialization(notaryConfig, btcAddressesProvider).init()
        }
        .failure { ex ->
            logger.logger.error("cannot run btc notary", ex)
            System.exit(1)
        }
}
