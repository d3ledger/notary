@file:JvmName("BtcRegistrationMain")

package registration.btc

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.loadConfigs
import mu.KLogging
import sidechain.iroha.IrohaInitialization

/**
 * Entry point for Registration Service
 */
fun main(args: Array<String>) {
    val registrationConfig =
        loadConfigs("btc-registration", BtcRegistrationConfig::class.java, "/btc/registration.properties")
    executeRegistration(registrationConfig)
}

fun executeRegistration(registrationConfig: BtcRegistrationConfig) {
    val logger = KLogging()
    IrohaInitialization.loadIrohaLibrary()
        .flatMap { BtcRegistrationServiceInitialization(registrationConfig).init() }
        .failure { ex ->
            logger.logger.error("cannot run btc registration", ex)
            System.exit(1)
        }
}
