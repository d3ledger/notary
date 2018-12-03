@file:JvmName("EthSendEther")

package deploy

import config.EthereumConfig
import config.loadConfigs
import config.loadEthPasswords
import mu.KLogging
import sidechain.eth.util.DeployHelper
import java.io.File
import java.math.BigInteger

private val logger = KLogging().logger

/**
 * Send ethe.
 * [args] should contain the address and amount of ether to send from genesis account
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        logger.error { "No arguments provided." }
        System.exit(1)
    }
    val addr = args[0]
    val amount = args[1].toDouble()
    logger.info { "Send ether $amount from genesis to $addr" }

    val ethereumConfig = loadConfigs("predeploy.ethereum", EthereumConfig::class.java, "/eth/predeploy.properties")
    val passwordConfig = loadEthPasswords("predeploy", "/eth/ethereum_password.properties")
    val deployHelper = DeployHelper(ethereumConfig, passwordConfig)

    deployHelper.sendEthereum(BigInteger.valueOf((1000000000000000000 * amount).toLong()), addr);
    logger.info { "Ether was sent" }

}
