@file:JvmName("EthRegistrationMain")

package registration.eth

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.ETH_RELAY_REGISTRY_ENV
import config.EthereumPasswords
import config.loadConfigs
import config.loadEthPasswords
import mu.KLogging
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaNetworkImpl

private val logger = KLogging().logger

/**
 * Entry point for Registration Service
 */
fun main(args: Array<String>) {
    val tmp =
        loadConfigs("eth-registration", EthRegistrationConfig::class.java, "/eth/registration.properties").get()

    val registrationConfig = object : EthRegistrationConfig {
        override val ethRelayRegistryAddress = System.getenv(ETH_RELAY_REGISTRY_ENV) ?: tmp.ethRelayRegistryAddress
        override val ethereum = tmp.ethereum
        override val port = tmp.port
        override val relayRegistrationIrohaAccount = tmp.relayRegistrationIrohaAccount
        override val notaryIrohaAccount = tmp.notaryIrohaAccount
        override val iroha = tmp.iroha
        override val registrationCredential = tmp.registrationCredential
    }

    val passwordConfig = loadEthPasswords("eth-registration", "/eth/ethereum_password.properties", args).get()

    executeRegistration(registrationConfig, passwordConfig)
}

fun executeRegistration(ethRegistrationConfig: EthRegistrationConfig, passwordConfig: EthereumPasswords) {
    logger.info { "Run ETH registration service" }
    val irohaNetwork = IrohaNetworkImpl(ethRegistrationConfig.iroha.hostname, ethRegistrationConfig.iroha.port)

    IrohaInitialization.loadIrohaLibrary()
        .flatMap {
            EthRegistrationServiceInitialization(ethRegistrationConfig, passwordConfig, irohaNetwork).init()

        }
        .failure { ex ->
            logger.error("cannot run eth registration", ex)
            irohaNetwork.close()
            System.exit(1)
        }
}
