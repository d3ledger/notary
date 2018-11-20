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
    val wdRollbackServiceConfig =
        loadConfigs("withdrawal", WdRollbackServiceConfig::class.java, "/eth/withdrawal.properties")

    executeWithdrawalRollback(wdRollbackServiceConfig)
}

fun executeWithdrawalRollback(
    wdRollbackServiceConfig: WdRollbackServiceConfig
) {
    logger.info { "Run withdrawal rollback service" }
    val irohaNetwork = IrohaNetworkImpl(wdRollbackServiceConfig.iroha.hostname, wdRollbackServiceConfig.iroha.port)

    IrohaInitialization.loadIrohaLibrary()
        .flatMap {
            ModelUtil.loadKeypair(
                wdRollbackServiceConfig.withdrawalCredential.pubkeyPath,
                wdRollbackServiceConfig.withdrawalCredential.privkeyPath
            )
        }
        .map { keypair -> IrohaCredential(wdRollbackServiceConfig.withdrawalCredential.accountId, keypair) }
        .flatMap { credential ->
            WdRollbackServiceInitialization(
                wdRollbackServiceConfig,
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
