@file:JvmName("WdRollbackServiceMain")

package wdrollbackservice

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.loadConfigs
import model.IrohaCredential
import mu.KLogging
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil

private val logger = KLogging().logger

/**
 * Main entry point of Withdrawal Rollback Service app
 */
fun main(args: Array<String>) {
    val withdrawalRollbackServiceConfig =
        loadConfigs("withdrawal_rollback", WithdrawalRollbackServiceConfig::class.java, "/eth/withdrawal_rollback.properties")

    executeWithdrawalRollback(withdrawalRollbackServiceConfig)
}

fun executeWithdrawalRollback(
    withdrawalRollbackServiceConfig: WithdrawalRollbackServiceConfig
) {
    logger.info { "Run withdrawal rollback service" }
    val irohaNetwork = IrohaNetworkImpl(withdrawalRollbackServiceConfig.iroha.hostname, withdrawalRollbackServiceConfig.iroha.port)

    IrohaInitialization.loadIrohaLibrary()
        .flatMap {
            ModelUtil.loadKeypair(
                withdrawalRollbackServiceConfig.withdrawalCredential.pubkeyPath,
                withdrawalRollbackServiceConfig.withdrawalCredential.privkeyPath
            )
        }
        .map { keypair -> IrohaCredential(withdrawalRollbackServiceConfig.withdrawalCredential.accountId, keypair) }
        .flatMap { credential ->
            WithdrawalRollbackServiceInitialization(
                withdrawalRollbackServiceConfig,
                irohaNetwork,
                credential
            ).init()
        }
        .failure { ex ->
            logger.error("Cannot run withdrawal rollback service", ex)
            irohaNetwork.close()
            System.exit(1)
        }
}
