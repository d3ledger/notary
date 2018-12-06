@file:JvmName("VacuumRelayMain")

package vacuum

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.loadConfigs
import config.loadEthPasswords
import model.IrohaCredential
import mu.KLogging
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil

private const val RELAY_VACUUM_PREFIX = "relay-vacuum"
private val logger = KLogging().logger
/**
 * Entry point for moving all currency from relay contracts to master contract
 */
fun main(args: Array<String>) {
    loadConfigs(RELAY_VACUUM_PREFIX, RelayVacuumConfig::class.java, "/eth/vacuum.properties")
        .map {
            executeVacuum(it, args)
        }
        .failure { ex ->
            logger.error("Cannot run vacuum", ex)
            System.exit(1)
        }
}

fun executeVacuum(relayVacuumConfig: RelayVacuumConfig, args: Array<String> = emptyArray()): Result<Unit, Exception> {
    logger.info { "Run relay vacuum" }
    val passwordConfig = loadEthPasswords(RELAY_VACUUM_PREFIX, "/eth/ethereum_password.properties", args).get()
    return IrohaInitialization.loadIrohaLibrary()
        .flatMap {
            ModelUtil.loadKeypair(
                relayVacuumConfig.vacuumCredential.pubkeyPath,
                relayVacuumConfig.vacuumCredential.privkeyPath
            )
        }
        .map { keypair -> IrohaCredential(relayVacuumConfig.vacuumCredential.accountId, keypair) }
        .flatMap { credential ->
            IrohaNetworkImpl(relayVacuumConfig.iroha.hostname, relayVacuumConfig.iroha.port).use { irohaNetwork ->
                RelayVacuum(relayVacuumConfig, passwordConfig, credential, irohaNetwork).vacuum()
            }
        }
}
