@file:JvmName("DeployRelayMain")

package registration.eth.relay

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.EthereumPasswords
import config.loadConfigs
import mu.KLogging
import sidechain.iroha.IrohaInitialization

/**
 * Entry point for deployment of relay smart contracts that will be used in client registration.
 * The main reason to move the logic of contract deployment to separate executable is that it takes too much time and
 * thus it should be done in advance.
 */
// TODO a.chernyshov - think about automatization of trigger and obtaining master address
fun main(args: Array<String>) {
    val logger = KLogging()

    val relayRegistrationConfig =
        loadConfigs("relay-registration", RelayRegistrationConfig::class.java, "/eth/relay_registration.properties")
    val passwordConfig =
        loadConfigs("relay-registration", EthereumPasswords::class.java, "/eth/ethereum_password.properties")

    IrohaInitialization.loadIrohaLibrary()
        .flatMap { RelayRegistration(relayRegistrationConfig, passwordConfig).deploy() }
        .failure { ex ->
            logger.logger.error("cannot run relay deployer", ex)
            System.exit(1)
        }
}
