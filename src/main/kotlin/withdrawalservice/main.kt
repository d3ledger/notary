@file:JvmName("WithdrawalServiceMain")

package withdrawalservice

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.EthereumPasswords
import config.loadConfigs
import config.loadEthPasswords
import model.IrohaCredential
import mu.KLogging
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.util.ModelUtil

private val logger = KLogging().logger

/**
 * Main entry point of Withdrawal Service app
 */
fun main(args: Array<String>) {
    val withdrawalConfig = loadConfigs("withdrawal", WithdrawalServiceConfig::class.java, "/eth/withdrawal.properties")
    val passwordConfig = loadEthPasswords("withdrawal", "/eth/ethereum_password.properties", args)
    executeWithdrawal(withdrawalConfig, passwordConfig)
}

fun executeWithdrawal(withdrawalConfig: WithdrawalServiceConfig, passwordConfig: EthereumPasswords) {
    logger.info { "Run withdrawal service" }
    IrohaInitialization.loadIrohaLibrary()
        .flatMap {
            ModelUtil.loadKeypair(
                withdrawalConfig.withdrawalCredential.pubkeyPath,
                withdrawalConfig.withdrawalCredential.privkeyPath
            )
        }
        .map { keypair -> IrohaCredential(withdrawalConfig.withdrawalCredential.accountId, keypair) }
        .flatMap { credential -> WithdrawalServiceInitialization(withdrawalConfig, credential, passwordConfig).init() }
        .failure { ex ->
            logger.error("Cannot run withdrawal service", ex)
            System.exit(1)
        }
}
