@file:JvmName("EthRegistrationMain")

package registration.eth

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.loadConfigs
import kotlinx.coroutines.experimental.launch
import mu.KLogging
import sidechain.iroha.IrohaInitialization

private val logger = KLogging().logger

/**
 * Entry point for Registration Service
 */
fun main(args: Array<String>) {
    val registrationConfig =
        loadConfigs("eth-registration", EthRegistrationConfig::class.java, "/eth/registration.properties")
    executeRegistration(registrationConfig)
}

fun executeRegistration(ethRegistrationConfig: EthRegistrationConfig) {
    logger.info { "Run ETH registration service" }
    IrohaInitialization.loadIrohaLibrary()
        .flatMap { EthRegistrationServiceInitialization(ethRegistrationConfig).init() }
        .failure { ex ->
            logger.error("cannot run eth registration", ex)
            System.exit(1)
        }

    val b = launch { }
}
