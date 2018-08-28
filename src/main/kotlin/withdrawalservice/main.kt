@file:JvmName("WithdrawalServiceMain")

package withdrawalservice

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.EthereumPasswords
import config.loadConfigs
import mu.KLogging
import sidechain.iroha.IrohaInitialization

/**
 * Main entry point of Withdrawal Service app
 */
fun main(args: Array<String>) {
    val withdrawalConfig = loadConfigs("withdrawal", WithdrawalServiceConfig::class.java, "/withdrawal.properties")
    val passwordConfig =
        loadConfigs("withdrawal", EthereumPasswords::class.java, "/ethereum_password.properties")
    executeWithdrawal(withdrawalConfig, passwordConfig)
}

fun executeWithdrawal(withdrawalConfig: WithdrawalServiceConfig, passwordConfig: EthereumPasswords) {
    val logger = KLogging()

    IrohaInitialization.loadIrohaLibrary()
        .flatMap { WithdrawalServiceInitialization(withdrawalConfig, passwordConfig).init() }
        .failure {
            logger.logger.error { it }
            System.exit(1)
        }

}
