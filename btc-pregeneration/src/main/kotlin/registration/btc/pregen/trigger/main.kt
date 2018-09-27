@file:JvmName("BtcPreGenerationTriggerMain")

package registration.btc.pregen.trigger

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.loadConfigs
import mu.KLogging
import provider.TriggerProvider
import provider.btc.BtcSessionProvider
import registration.btc.pregen.BtcPreGenConfig
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.util.ModelUtil
import util.getRandomId

private val logger = KLogging().logger

/*
This function is used to start BTC multisignature addresses pregeneration
 */
fun main(args: Array<String>) {
    val btcPkPreGenConfig =
        loadConfigs("btc-pregen", BtcPreGenConfig::class.java, "/pregeneration.properties")
    executeTrigger(btcPkPreGenConfig)
}

fun executeTrigger(btcPkPreGenConfig: BtcPreGenConfig) {
    logger.info { "Run BTC multisignature address pregeneration trigger" }
    IrohaInitialization.loadIrohaLibrary()
        .flatMap { ModelUtil.loadKeypair(btcPkPreGenConfig.iroha.pubkeyPath, btcPkPreGenConfig.iroha.privkeyPath) }
        .flatMap { keypair ->
            val triggerProvider = TriggerProvider(
                btcPkPreGenConfig.iroha,
                btcPkPreGenConfig.pubKeyTriggerAccount
            )
            val btcKeyGenSessionProvider = BtcSessionProvider(
                btcPkPreGenConfig.iroha,
                btcPkPreGenConfig.registrationAccount,
                keypair
            )
            val sessionAccountName = String.getRandomId()
            btcKeyGenSessionProvider.createPubKeyCreationSession(sessionAccountName)
                .map { triggerProvider.trigger(sessionAccountName) }
        }.failure { ex ->
            logger.error("Cannot trigger btc address pregeneration", ex)
            System.exit(1)
        }
}
