@file:JvmName("ERC20TokenRegistrationMain")

package token

import com.github.kittinunf.result.flatMap
import config.loadConfigs
import mu.KLogging
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.util.ModelUtil

private val logger = KLogging().logger

/**
 * ERC20 tokens registration entry point
 */
fun main(args: Array<String>) {
    val tokenRegistrationConfig =
        loadConfigs(
            "token-registration",
            ERC20TokenRegistrationConfig::class.java,
            "/eth/token_registration.properties"
        )
    executeTokenRegistration(tokenRegistrationConfig)
}

fun executeTokenRegistration(tokenRegistrationConfig: ERC20TokenRegistrationConfig) {
    logger.info { "Run ERC20 tokens registration" }
    IrohaInitialization.loadIrohaLibrary()
        .flatMap {
            ModelUtil.loadKeypair(
                tokenRegistrationConfig.iroha.pubkeyPath,
                tokenRegistrationConfig.iroha.privkeyPath
            )
        }
        .flatMap { keypair -> ERC20TokenRegistration(keypair, tokenRegistrationConfig).init() }
        .fold({ logger.info { "ERC20 tokens were successfully registered" } }, { ex ->
            logger.error("Cannot run ERC20 token registration", ex)
            System.exit(1)
        })
}
