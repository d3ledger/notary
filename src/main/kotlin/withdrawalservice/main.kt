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
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import vacuum.RelayVacuumConfig

private val logger = KLogging().logger

private const val RELAY_VACUUM_PREFIX = "relay-vacuum"

/**
 * Main entry point of Withdrawal Service app
 */
fun main(args: Array<String>) {
    val withdrawalConfig = loadConfigs("withdrawal", WithdrawalServiceConfig::class.java, "/eth/withdrawal.properties").get()
    val passwordConfig = loadEthPasswords("withdrawal", "/eth/ethereum_password.properties", args).get()
    val relayVacuumConfig =
        loadConfigs(RELAY_VACUUM_PREFIX, RelayVacuumConfig::class.java, "/eth/vacuum.properties").get()
    executeWithdrawal(withdrawalConfig, passwordConfig, relayVacuumConfig)
}

fun executeWithdrawal(
    withdrawalConfig: WithdrawalServiceConfig,
    passwordConfig: EthereumPasswords,
    relayVacuumConfig: RelayVacuumConfig
) {
    logger.info { "Run withdrawal service" }
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
            WithdrawalServiceInitialization(
                withdrawalConfig,
                credential,
                irohaNetwork,
                passwordConfig,
                relayVacuumConfig
            ).init()
        }
        .failure { ex ->
            logger.error("Cannot run withdrawal service", ex)
            irohaNetwork.close()
            System.exit(1)
        }
}
