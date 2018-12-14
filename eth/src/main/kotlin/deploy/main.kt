@file:JvmName("EthPreDeployMain")

package deploy

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.map
import config.EthereumConfig
import config.loadConfigs
import config.loadEthPasswords
import mu.KLogging
import sidechain.eth.util.DeployHelper
import java.io.File

private val logger = KLogging().logger

/**
 * Entry point to deploy smart contracts.
 * [args] should contain the list of notary ethereum addresses
 */
fun main(args: Array<String>) {
    logger.info { "Run predeploy with notary addresses: ${args.toList()}" }
    if (args.isEmpty()) {
        logger.error { "No notary ethereum addresses are provided." }
        System.exit(1)
    }

    loadConfigs("predeploy.ethereum", EthereumConfig::class.java, "/eth/predeploy.properties")
        .fanout { loadEthPasswords("predeploy", "/eth/ethereum_password.properties") }
        .map { (ethereumConfig, passwordConfig) -> DeployHelper(ethereumConfig, passwordConfig) }
        .map { deployHelper ->

            val relayRegistry = deployHelper.deployRelayRegistrySmartContract()
            val master = deployHelper.deployMasterSmartContract(relayRegistry.contractAddress)

            var result = master.addPeers(args.toList()).send().isStatusOK
            logger.info { "Peers were added" }

            result = result && master.disableAddingNewPeers().send().isStatusOK

            logger.info { "Master account has been locked" }

            if (!result) {
                logger.error("Error: failed to call master smart contract")
                System.exit(1)
            }

            File("master_eth_address").printWriter().use {
                it.print(master.contractAddress)
            }
            File("relay_registry_eth_address").printWriter().use {
                it.print(relayRegistry.contractAddress)
            }
        }
        .failure { ex ->
            logger.error("Cannot deploy smart contract", ex)
            System.exit(1)
        }
}
