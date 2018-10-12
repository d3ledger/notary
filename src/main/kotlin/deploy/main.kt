@file:JvmName("EthPreDeployMain")

package deploy

import config.EthereumConfig
import config.loadConfigs
import config.loadEthPasswords
import mu.KLogging
import sidechain.eth.util.DeployHelper

private val logger = KLogging().logger

/**
 * Entry point to deploy smart contracts.
 * [args] should contain the list of notary ethereum addresses
 */
fun main(args: Array<String>) {
    args.forEach { println(it) }
    logger.info { "Run predeploy with notary addresses: ${args.toList()}" }

    val ethereumConfig = loadConfigs("predeploy.ethereum", EthereumConfig::class.java, "/eth/predeploy.properties")
    val passwordConfig = loadEthPasswords("eth-registration", "/eth/ethereum_password.properties")
    val deployHelper = DeployHelper(ethereumConfig, passwordConfig)

    val relayRegistry = deployHelper.deployRelayRegistrySmartContract()
    val master = deployHelper.deployMasterSmartContract(relayRegistry.contractAddress)

    var result = true
    args.forEach { address ->
        result = result && master.addPeer(address).send().isStatusOK
    }
    result = result && master.disableAddingNewPeers().send().isStatusOK

    if (!result) {
        logger.error("Error: failed to call master smart contract")
        System.exit(1)
    }
}
