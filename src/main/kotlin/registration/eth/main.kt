@file:JvmName("EthRegistrationMain")

package registration.eth

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.EthereumPasswords
import config.loadConfigs
import config.loadEthPasswords
import mu.KLogging
import sidechain.iroha.IrohaInitialization

private val logger = KLogging().logger

/**
 * Entry point for Registration Service
 */
fun main(args: Array<String>) {
    val registrationConfig =
        loadConfigs("eth-registration", EthRegistrationConfig::class.java, "/eth/registration.properties")
    val passwordConfig = loadEthPasswords("eth-registration", "/eth/ethereum_password.properties", args)

    executeRegistration(registrationConfig, passwordConfig)
}

fun executeRegistration(ethRegistrationConfig: EthRegistrationConfig, passwordConfig: EthereumPasswords) {
    logger.info { "Run ETH registration service" }
    IrohaInitialization.loadIrohaLibrary()
        .flatMap { EthRegistrationServiceInitialization(ethRegistrationConfig, passwordConfig).init() }
        .failure { ex ->
            logger.error("cannot run eth registration", ex)
            System.exit(1)
        }
}
