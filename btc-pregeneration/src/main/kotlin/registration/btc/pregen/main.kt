@file:JvmName("BtcPreGenerationMain")

package registration.btc.pregen

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.loadConfigs
import model.IrohaCredential
import mu.KLogging
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.util.ModelUtil

private val logger = KLogging().logger
fun main(args: Array<String>) {
    val btcPkPreGenConfig =
        loadConfigs("btc-pregen", BtcPreGenConfig::class.java, "/pregeneration.properties")
    executePreGeneration(btcPkPreGenConfig)
}

fun executePreGeneration(btcPkPreGenConfig: BtcPreGenConfig) {
    logger.info { "Run BTC multisignature address pregeneration" }
    IrohaInitialization.loadIrohaLibrary()
        .map {
            Pair(
                ModelUtil.loadKeypair(
                    btcPkPreGenConfig.registrationAccount.pubkeyPath,
                    btcPkPreGenConfig.registrationAccount.privkeyPath
                ),
                ModelUtil.loadKeypair(
                    btcPkPreGenConfig.mstRegistrationAccount.pubkeyPath,
                    btcPkPreGenConfig.mstRegistrationAccount.privkeyPath
                )
            )
        }.map { (regKeyPair, mstRegKeyPair) ->
            Pair(
                IrohaCredential(btcPkPreGenConfig.registrationAccount.accountId, regKeyPair.get()),
                IrohaCredential(btcPkPreGenConfig.registrationAccount.accountId, mstRegKeyPair.get())
            )
        }
        .flatMap { (regCredential, mstRegCredential) ->
            BtcPreGenInitialization(regCredential,mstRegCredential, btcPkPreGenConfig).init() }
        .failure { ex ->
            logger.error("cannot run btc address pregeneration", ex)
            System.exit(1)
        }
}
