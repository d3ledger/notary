@file:JvmName("VacuumRelayMain")

package vacuum

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.EthereumPasswords
import config.loadConfigs
import mu.KLogging
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.util.ModelUtil

private const val RELAY_VACUUM_PREFIX = "relay-vacuum"

/**
 * Entry point for moving all currency from relay contracts to master contract
 */
fun main(args: Array<String>) {
    val logger = KLogging()
    val relayVacuumConfig = loadConfigs(RELAY_VACUUM_PREFIX, RelayVacuumConfig::class.java)
    val passwordConfig =
        loadConfigs(RELAY_VACUUM_PREFIX, EthereumPasswords::class.java, "/ethereum_password.properties")
    IrohaInitialization.loadIrohaLibrary()
        .flatMap { ModelUtil.loadKeypair(relayVacuumConfig.iroha.pubkeyPath, relayVacuumConfig.iroha.privkeyPath) }
        .flatMap { keypair -> RelayVacuum(relayVacuumConfig, passwordConfig, keypair).vacuum() }
        .failure { ex ->
            logger.logger.error { ex }
            System.exit(1)
        }
}
