@file:JvmName("EthSendEther")

package deploy

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.map
import config.EthereumConfig
import config.loadConfigs
import config.loadEthPasswords
import mu.KLogging
import sidechain.eth.util.DeployHelper
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


    loadConfigs("predeploy.ethereum", EthereumConfig::class.java, "/eth/predeploy.properties")
            .fanout { loadEthPasswords("predeploy", "/eth/ethereum_password.properties") }
            .map { (ethereumConfig, passwordConfig) -> DeployHelper(ethereumConfig, passwordConfig) }
            .map { deployHelper ->
                deployHelper.sendEthereum(BigInteger.valueOf((1000000000000000000 * amount).toLong()), addr);
                logger.info { "Ether was sent" }

            }
            .failure { ex ->
                logger.error("Cannot send eth", ex)
                System.exit(1)
            }


}
