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
        .map {
            object : EthRegistrationConfig {
                override val ethRelayRegistryAddress = System.getenv(ETH_RELAY_REGISTRY_ENV)
                    ?: it.ethRelayRegistryAddress
                override val ethereum = it.ethereum
                override val port = it.port
                override val relayRegistrationIrohaAccount = it.relayRegistrationIrohaAccount
                override val notaryIrohaAccount = it.notaryIrohaAccount
                override val iroha = it.iroha
                override val registrationCredential = it.registrationCredential
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
