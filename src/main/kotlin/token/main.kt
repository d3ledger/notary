@file:JvmName("ERC20TokenRegistrationMain")

package token

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.loadConfigs
import mu.KLogging
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.util.ModelUtil

/**
 * ERC20 tokens registration entry point
 */
fun main(args: Array<String>) {
    val tokenRegistrationConfig =
        loadConfigs("token-registration", ERC20TokenRegistrationConfig::class.java, "/eth/token_registration.properties")
    executeTokenRegistration(tokenRegistrationConfig)
}

fun executeTokenRegistration(tokenRegistrationConfig: ERC20TokenRegistrationConfig) {
    val logger = KLogging()
    IrohaInitialization.loadIrohaLibrary()
        .flatMap {
            ModelUtil.loadKeypair(
                tokenRegistrationConfig.iroha.pubkeyPath,
                tokenRegistrationConfig.iroha.privkeyPath
            )
        }
        .flatMap { keypair -> ERC20TokenRegistration(keypair, tokenRegistrationConfig).init() }
        .failure { ex ->
            logger.logger.error("cannot run ERC20 token registration", ex)
            System.exit(1)
        }
}
