@file:JvmName("DeployRelayMain")

package registration.eth.relay

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.ETH_MASTER_WALLET_ENV
import config.ETH_RELAY_IMPLEMENTATION_ADDRESS_ENV
import config.loadConfigs
import config.loadEthPasswords
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import model.IrohaCredential
import mu.KLogging
import provider.eth.EthFreeRelayProvider
import sidechain.iroha.util.ModelUtil

private val logger = KLogging().logger

/**
 * Entry point for deployment of relay smart contracts that will be used in client registration.
 * The main reason to move the logic of contract deployment to separate executable is that it takes too much time and
 * thus it should be done in advance.
 * Every [relayRegistrationConfig.replenishmentPeriod] checks that number of free relays is
 * [relayRegistrationConfig.number]. In case of lack of free relays
 */
fun main(args: Array<String>) {
    logger.info { "Run relay deployment" }
    loadConfigs("relay-registration", RelayRegistrationConfig::class.java, "/eth/relay_registration.properties")
        .map { relayRegistrationConfig ->
            object : RelayRegistrationConfig {
                override val number = relayRegistrationConfig.number
                override val replenishmentPeriod = relayRegistrationConfig.replenishmentPeriod
                override val ethMasterWallet =
                    System.getenv(ETH_MASTER_WALLET_ENV) ?: relayRegistrationConfig.ethMasterWallet
                override val ethRelayImplementationAddress =
                    System.getenv(ETH_RELAY_IMPLEMENTATION_ADDRESS_ENV)
                        ?: relayRegistrationConfig.ethRelayImplementationAddress
                override val notaryIrohaAccount = relayRegistrationConfig.notaryIrohaAccount
                override val relayRegistrationCredential = relayRegistrationConfig.relayRegistrationCredential
                override val iroha = relayRegistrationConfig.iroha
                override val ethereum = relayRegistrationConfig.ethereum
            }
        }
        .fanout { loadEthPasswords("relay-registration", "/eth/ethereum_password.properties", args) }
        .map { (relayRegistrationConfig, passwordConfig) ->
            ModelUtil.loadKeypair(
                relayRegistrationConfig.relayRegistrationCredential.pubkeyPath,
                relayRegistrationConfig.relayRegistrationCredential.privkeyPath
            )
                .map { keypair ->
                    IrohaCredential(
                        relayRegistrationConfig.relayRegistrationCredential.accountId,
                        keypair
                    )
                }
                .flatMap { credential ->
                    IrohaAPI(
                        relayRegistrationConfig.iroha.hostname,
                        relayRegistrationConfig.iroha.port
                    ).use { irohaAPI ->
                        val queryAPI = QueryAPI(irohaAPI, credential.accountId, credential.keyPair)
                        val freeRelayProvider = EthFreeRelayProvider(
                            queryAPI,
                            relayRegistrationConfig.notaryIrohaAccount,
                            relayRegistrationConfig.relayRegistrationCredential.accountId
                        )

                        val relayRegistration = RelayRegistration(
                            freeRelayProvider,
                            relayRegistrationConfig,
                            credential,
                            irohaAPI,
                            passwordConfig
                        )

                        if (args.isEmpty()) {
                            relayRegistration.runRelayReplenishment()
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
