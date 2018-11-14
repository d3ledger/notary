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
import withdrawalservice.WithdrawalServiceConfig

private val logger = KLogging().logger

/**
 * Main entry point of Withdrawal Rollback Service app
 */
fun main(args: Array<String>) {
    val withdrawalConfig = loadConfigs("withdrawal", WithdrawalServiceConfig::class.java, "/eth/withdrawal.properties")

    executeWithdrawal(withdrawalConfig)
}

fun executeWithdrawal(
    withdrawalConfig: WithdrawalServiceConfig
) {
    logger.info { "Run withdrawal rollback service" }
    val irohaNetwork = IrohaNetworkImpl(withdrawalConfig.iroha.hostname, withdrawalConfig.iroha.port)

    IrohaInitialization.loadIrohaLibrary()
        .flatMap {
            ModelUtil.loadKeypair(
                withdrawalConfig.withdrawalCredential.pubkeyPath,
                withdrawalConfig.withdrawalCredential.privkeyPath
            )
        }
        .map { keypair -> IrohaCredential(withdrawalConfig.withdrawalCredential.accountId, keypair) }
        .flatMap { credential ->
            WdRollbackServiceInitialization(
                withdrawalConfig,
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
