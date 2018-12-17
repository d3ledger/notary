@file:JvmName("DeployRelayMain")

package registration.eth.relay

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.ETH_MASTER_WALLET_ENV
import config.loadConfigs
import config.loadEthPasswords
import model.IrohaCredential
import mu.KLogging
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil

private val logger = KLogging().logger

/**
 * Entry point for deployment of relay smart contracts that will be used in client registration.
 * The main reason to move the logic of contract deployment to separate executable is that it takes too much time and
 * thus it should be done in advance.
 */
// TODO a.chernyshov - think about automatization of trigger and obtaining master address
fun main(args: Array<String>) {
    logger.info { "Run relay deployment" }
    loadConfigs("relay-registration", RelayRegistrationConfig::class.java, "/eth/relay_registration.properties")
        .map { relayRegistrationConfig ->
            object : RelayRegistrationConfig {
                override val number = relayRegistrationConfig.number
                override val ethMasterWallet =
                    System.getenv(ETH_MASTER_WALLET_ENV) ?: relayRegistrationConfig.ethMasterWallet
                override val notaryIrohaAccount = relayRegistrationConfig.notaryIrohaAccount
                override val relayRegistrationCredential = relayRegistrationConfig.relayRegistrationCredential
                override val iroha = relayRegistrationConfig.iroha
                override val ethereum = relayRegistrationConfig.ethereum
            }
        }
        .fanout { loadEthPasswords("relay-registration", "/eth/ethereum_password.properties", args) }
        .map { (relayRegistrationConfig, passwordConfig) ->
            IrohaInitialization.loadIrohaLibrary()
                .flatMap {
                    ModelUtil.loadKeypair(
                        relayRegistrationConfig.relayRegistrationCredential.pubkeyPath,
                        relayRegistrationConfig.relayRegistrationCredential.privkeyPath
                    )
                }
                .map { keypair ->
                    IrohaCredential(
                        relayRegistrationConfig.relayRegistrationCredential.accountId,
                        keypair
                    )
                }
                .flatMap { credential ->
                    IrohaNetworkImpl(
                        relayRegistrationConfig.iroha.hostname,
                        relayRegistrationConfig.iroha.port
                    ).use { irohaNetwork ->
                        val relayRegistration =
                            RelayRegistration(relayRegistrationConfig, credential, irohaNetwork, passwordConfig)
                        if (args.isEmpty()) {
                            relayRegistration.deploy()
                        } else {
                            relayRegistration.import(args[0])
                        }
                    }
                }.failure { ex ->
                    logger.error("Cannot run relay deployer", ex)
                    System.exit(1)
                }
        }
}
