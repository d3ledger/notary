@file:JvmName("ERC20TokenRegistrationMain")

package token

import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import com.d3.commons.config.loadConfigs
import jp.co.soramitsu.iroha.java.IrohaAPI
import com.d3.commons.model.IrohaCredential
import mu.KLogging
import com.d3.commons.sidechain.iroha.util.ModelUtil

private val logger = KLogging().logger

/**
 * ERC20 tokens registration entry point
 */
fun main(args: Array<String>) {
    loadConfigs(
        "token-registration",
        ERC20TokenRegistrationConfig::class.java,
        "/eth/token_registration.properties"
    ).map { executeTokenRegistration(it) }
}

fun executeTokenRegistration(tokenRegistrationConfig: ERC20TokenRegistrationConfig) {
    logger.info { "Run ERC20 tokens registration" }
    ModelUtil.loadKeypair(
        tokenRegistrationConfig.irohaCredential.pubkeyPath,
        tokenRegistrationConfig.irohaCredential.privkeyPath
    )
        .map { keypair -> IrohaCredential(tokenRegistrationConfig.irohaCredential.accountId, keypair) }
        .flatMap { credentials ->
            IrohaAPI(
                tokenRegistrationConfig.iroha.hostname,
                tokenRegistrationConfig.iroha.port
            ).use { irohaNetwork ->
                ERC20TokenRegistration(tokenRegistrationConfig, credentials, irohaNetwork).init()
            }
        }
        .fold(
            {
                logger.info { "ERC20 tokens were successfully registered" }
            },
            { ex ->
                logger.error("Cannot run ERC20 token registration", ex)
                System.exit(1)
            }
        )
}
