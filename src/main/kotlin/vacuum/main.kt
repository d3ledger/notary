@file:JvmName("VacuumRelayMain")

package vacuum

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.loadConfigs
import config.loadEthPasswords
import mu.KLogging
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.util.ModelUtil

private const val RELAY_VACUUM_PREFIX = "relay-vacuum"

/**
 * Entry point for moving all currency from relay contracts to master contract
 */
fun main(args: Array<String>) {
    val relayVacuumConfig = loadConfigs(RELAY_VACUUM_PREFIX, RelayVacuumConfig::class.java, "/eth/vacuum.properties")
    executeVacuum(relayVacuumConfig, args)
}

fun executeVacuum(relayVacuumConfig: RelayVacuumConfig, args: Array<String> = emptyArray()) {
    val logger = KLogging()
    val passwordConfig = loadEthPasswords(RELAY_VACUUM_PREFIX, "/eth/ethereum_password.properties", args)
    IrohaInitialization.loadIrohaLibrary()
        .flatMap { ModelUtil.loadKeypair(relayVacuumConfig.iroha.pubkeyPath, relayVacuumConfig.iroha.privkeyPath) }
        .flatMap { keypair -> RelayVacuum(relayVacuumConfig, passwordConfig, keypair).vacuum() }
        .failure { ex ->
            logger.logger.error("cannot run vacuum", ex)
            System.exit(1)
        }
}
