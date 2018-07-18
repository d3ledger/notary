@file:JvmName("WithdrawalServiceMain")

package withdrawalservice

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.loadConfigs
import mu.KLogging
import sidechain.iroha.IrohaInitialization


/**
 * Main entry point of Withdrawal Service app
 */
fun main(args: Array<String>) {
    val logger = KLogging()

    val withdrawalConfig = loadConfigs("withdrawal", WithdrawalServiceConfig::class.java)

    IrohaInitialization.loadIrohaLibrary()
        .flatMap { WithdrawalServiceInitialization(withdrawalConfig).init() }
        .failure {
            logger.logger.error { it }
            System.exit(1)
        }

}
