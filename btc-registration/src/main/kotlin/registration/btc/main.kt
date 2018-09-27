@file:JvmName("BtcRegistrationMain")

package registration.btc

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.loadConfigs
import model.IrohaCredential
import mu.KLogging
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.util.ModelUtil

private val logger = KLogging().logger

/**
 * Entry point for Registration Service
 */
fun main(args: Array<String>) {
    val registrationConfig =
        loadConfigs("btc-registration", BtcRegistrationConfig::class.java, "/registration.properties")
    executeRegistration(registrationConfig)
}

fun executeRegistration(registrationConfig: BtcRegistrationConfig) {
    logger.info { "Run BTC client registration" }
    IrohaInitialization.loadIrohaLibrary()
        .flatMap {
            ModelUtil.loadKeypair(
                registrationConfig.registrationCredential.pubkeyPath,
                registrationConfig.registrationCredential.privkeyPath
            )
        }
        .map { keypair -> IrohaCredential(registrationConfig.registrationCredential.accountId, keypair)}
        .flatMap {credential ->  BtcRegistrationServiceInitialization(registrationConfig, credential).init() }
        .failure { ex ->
            logger.error("Cannot run btc registration", ex)
            System.exit(1)
        }
}
