@file:JvmName("BtcNotaryMain")

package notary.btc

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
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
    IrohaInitialization.loadIrohaLibrary()
        .flatMap {
            ModelUtil.loadKeypair(
                notaryConfig.notaryCredential.pubkeyPath,
                notaryConfig.notaryCredential.pubkeyPath
            )
        }.map { keypair ->
            IrohaCredential(
                notaryConfig.notaryCredential.accountId, keypair
            )
        }
        .flatMap { credential ->
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
