@file:JvmName("BtcPreGenerationTriggerMain")

package pregeneration.btc.trigger

import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import model.IrohaCredential
import mu.KLogging
import pregeneration.btc.config.BtcPreGenConfig
import pregeneration.btc.config.btcPreGenConfig
import provider.TriggerProvider
import provider.btc.BtcSessionProvider
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import util.getRandomId

private val logger = KLogging().logger

/**
 * This function is used to start BTC multisignature addresses pregeneration
 */
fun main(args: Array<String>) {
    executeTrigger(btcPreGenConfig)
}

fun executeTrigger(btcPkPreGenConfig: BtcPreGenConfig) {
    logger.info { "Run BTC multisignature address pregeneration trigger" }
    IrohaInitialization.loadIrohaLibrary()
        .flatMap {
            ModelUtil.loadKeypair(
                btcPkPreGenConfig.registrationAccount.pubkeyPath,
                btcPkPreGenConfig.registrationAccount.privkeyPath
            )
        }.map { keypair ->
            IrohaCredential(btcPkPreGenConfig.registrationAccount.accountId, keypair)
        }
        .flatMap { credential ->
            IrohaNetworkImpl(btcPkPreGenConfig.iroha.hostname, btcPkPreGenConfig.iroha.port).use { irohaNetwork ->
                val triggerProvider = TriggerProvider(
                    credential,
                    irohaNetwork,
                    btcPkPreGenConfig.pubKeyTriggerAccount
                )
                val btcKeyGenSessionProvider = BtcSessionProvider(
                    credential,
                    irohaNetwork
                )
                val sessionAccountName = String.getRandomId()
                btcKeyGenSessionProvider.createPubKeyCreationSession(sessionAccountName)
                    .map { triggerProvider.trigger(sessionAccountName) }
            }
        }.fold(
            { logger.info { "BTC multisignature address registration service was successfully triggered" } },
            { ex ->
                logger.error("Cannot trigger btc address pregeneration", ex)
                System.exit(1)
            })
}
