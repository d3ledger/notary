@file:JvmName("WithdrawalServiceMain")

package withdrawalservice

import com.github.kittinunf.result.*
import config.*
import io.reactivex.disposables.Disposable
import jp.co.soramitsu.iroha.java.IrohaAPI
import model.IrohaCredential
import mu.KLogging
import sidechain.iroha.util.ModelUtil
import vacuum.RelayVacuumConfig

private val logger = KLogging().logger

private const val RELAY_VACUUM_PREFIX = "relay-vacuum"

/**
 * Main entry point of Withdrawal Service app
 */
fun main(args: Array<String>) {
    loadConfigs("withdrawal", WithdrawalServiceConfig::class.java, "/eth/withdrawal.properties")
        .fanout { loadEthPasswords("withdrawal", "/eth/ethereum_password.properties", args) }
        .map { (withdrawalConfig, passwordConfig) ->
            loadConfigs(RELAY_VACUUM_PREFIX, RelayVacuumConfig::class.java, "/eth/vacuum.properties")
                .flatMap { relayVacuumConfig ->
                    val rmqConfig = loadRawConfigs("rmq", RMQConfig::class.java, "${getConfigFolder()}/rmq.properties")
                    executeWithdrawal(withdrawalConfig, passwordConfig, relayVacuumConfig, rmqConfig)
                }
        }
        .failure { ex ->
            logger.error("Cannot run withdrawal service", ex)
            System.exit(1)
        }
}

fun executeWithdrawal(
    withdrawalConfig: WithdrawalServiceConfig,
    passwordConfig: EthereumPasswords,
    relayVacuumConfig: RelayVacuumConfig,
    rmqConfig: RMQConfig
): Result<Disposable, Exception> {
    logger.info { "Run withdrawal service" }
    val irohaAPI = IrohaAPI(withdrawalConfig.iroha.hostname, withdrawalConfig.iroha.port)

    return ModelUtil.loadKeypair(
        withdrawalConfig.withdrawalCredential.pubkeyPath,
        withdrawalConfig.withdrawalCredential.privkeyPath
    )
        .map { keypair -> IrohaCredential(withdrawalConfig.withdrawalCredential.accountId, keypair) }
        .flatMap { credential ->
            WithdrawalServiceInitialization(
                withdrawalConfig,
                credential,
                irohaAPI,
                passwordConfig,
                relayVacuumConfig,
                rmqConfig
            ).init()
        }
}
