@file:JvmName("EthRegistrationMain")

package registration.eth

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
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
    loadConfigs("eth-registration", EthRegistrationConfig::class.java, "/eth/registration.properties")
        .map { ethRegistrationConfig ->
            object : EthRegistrationConfig {
                override val ethRelayRegistryAddress = System.getenv(ETH_RELAY_REGISTRY_ENV)
                    ?: ethRegistrationConfig.ethRelayRegistryAddress
                override val ethereum = ethRegistrationConfig.ethereum
                override val port = ethRegistrationConfig.port
                override val relayRegistrationIrohaAccount = ethRegistrationConfig.relayRegistrationIrohaAccount
                override val notaryIrohaAccount = ethRegistrationConfig.notaryIrohaAccount
                override val iroha = ethRegistrationConfig.iroha
                override val registrationCredential = ethRegistrationConfig.registrationCredential
            }
        }
        .fanout { loadEthPasswords("eth-registration", "/eth/ethereum_password.properties", args) }
        .map { (registrationConfig, passwordConfig) ->
            executeRegistration(registrationConfig, passwordConfig)
        }
}

fun executeRegistration(ethRegistrationConfig: EthRegistrationConfig, passwordConfig: EthereumPasswords) {
    logger.info { "Run ETH registration service" }
    val irohaNetwork = IrohaNetworkImpl(ethRegistrationConfig.iroha.hostname, ethRegistrationConfig.iroha.port)

    IrohaInitialization.loadIrohaLibrary()
        .flatMap { EthRegistrationServiceInitialization(ethRegistrationConfig, passwordConfig, irohaNetwork).init() }
        .failure { ex ->
            logger.error("cannot run eth registration", ex)
            irohaNetwork.close()
            System.exit(1)
        }
}
