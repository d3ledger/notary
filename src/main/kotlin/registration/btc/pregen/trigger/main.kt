@file:JvmName("BtcPreGenerationTriggerMain")

package registration.btc.pregen.trigger

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.loadConfigs
import model.IrohaCredential
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
        loadConfigs("btc-pregen", BtcPreGenConfig::class.java, "/btc/pregeneration.properties")
    executeTrigger(btcPkPreGenConfig)
}

fun executeTrigger(btcPkPreGenConfig: BtcPreGenConfig) {
    logger.info { "Run BTC multisignature address pregeneration trigger" }
    IrohaInitialization.loadIrohaLibrary()
        .flatMap {
            ModelUtil.loadKeypair(
                btcPkPreGenConfig.registrationAccount.pubkeyPath,
                btcPkPreGenConfig.registrationAccount.privkeyPath
            )
        }.map {keypair ->
            IrohaCredential(btcPkPreGenConfig.registrationAccount.accountId, keypair)
        }
        .flatMap { credential ->
            val triggerProvider = TriggerProvider(
                btcPkPreGenConfig.iroha,
                credential,
                btcPkPreGenConfig.pubKeyTriggerAccount
            )
            val btcKeyGenSessionProvider = BtcSessionProvider(
                btcPkPreGenConfig.iroha,
                credential
            )
            val sessionAccountName = String.getRandomId()
            btcKeyGenSessionProvider.createPubKeyCreationSession(sessionAccountName)
                .map { triggerProvider.trigger(sessionAccountName) }
        }.failure { ex ->
            logger.error("Cannot trigger btc address pregeneration", ex)
            System.exit(1)
        }
}
