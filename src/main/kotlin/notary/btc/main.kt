@file:JvmName("BtcNotaryMain")

package notary.btc

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.loadConfigs
import model.IrohaCredential
import mu.KLogging
import provider.btc.BtcRegisteredAddressesProvider
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.util.ModelUtil

private val logger = KLogging().logger

fun main(args: Array<String>) {
    val notaryConfig = loadConfigs("btc-notary", BtcNotaryConfig::class.java, "/btc/notary.properties")
    executeNotary(notaryConfig)
}

fun executeNotary(notaryConfig: BtcNotaryConfig) {
    logger.info { "Run BTC notary" }

    val credential = IrohaCredential(
        notaryConfig.notaryCredential.accountId,
        ModelUtil.loadKeypair(
            notaryConfig.notaryCredential.pubkeyPath,
            notaryConfig.notaryCredential.pubkeyPath
        ).get())

    IrohaInitialization.loadIrohaLibrary().flatMap {
            val btcTakenAddressesProvider = BtcRegisteredAddressesProvider(
                notaryConfig.iroha,
                credential,
                notaryConfig.registrationAccount,
                credential.accountId
            )
            BtcNotaryInitialization(notaryConfig, btcTakenAddressesProvider).init()
        }
        .failure { ex ->
            logger.error("Cannot run btc notary", ex)
            System.exit(1)
        }
}
