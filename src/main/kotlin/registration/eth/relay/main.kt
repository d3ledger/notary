@file:JvmName("DeployRelayMain")

package registration.eth.relay

import com.github.kittinunf.result.failure
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
    val tmp =
        loadConfigs("relay-registration", RelayRegistrationConfig::class.java, "/eth/relay_registration.properties")

    val relayRegistrationConfig = object : RelayRegistrationConfig {
        override val number = tmp.number
        override val ethMasterWallet = System.getenv(ETH_MASTER_WALLET_ENV) ?: tmp.ethMasterWallet
        override val notaryIrohaAccount = tmp.notaryIrohaAccount
        override val relayRegistrationCredential = tmp.relayRegistrationCredential
        override val iroha = tmp.iroha
        override val ethereum = tmp.ethereum

    }

    val passwordConfig = loadEthPasswords("relay-registration", "/eth/ethereum_password.properties", args)

    IrohaInitialization.loadIrohaLibrary()
        .flatMap {
            ModelUtil.loadKeypair(
                relayRegistrationConfig.relayRegistrationCredential.pubkeyPath,
                relayRegistrationConfig.relayRegistrationCredential.privkeyPath
            )
        }
        .map { keypair -> IrohaCredential(relayRegistrationConfig.relayRegistrationCredential.accountId, keypair) }
        .flatMap { credential ->
            IrohaNetworkImpl(
                relayRegistrationConfig.iroha.hostname,
                relayRegistrationConfig.iroha.port
            ).use { irohaNetwork ->
                RelayRegistration(relayRegistrationConfig, credential, passwordConfig, irohaNetwork).deploy()
            }
        }.failure { ex ->
            logger.error("Cannot run relay deployer", ex)
            System.exit(1)
        }
}
