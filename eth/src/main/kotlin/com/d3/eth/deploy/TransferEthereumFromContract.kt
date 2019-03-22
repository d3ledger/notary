@file:JvmName("TransferEthereumFromContract")

package com.d3.eth.deploy

import com.d3.commons.config.EthereumConfig
import com.d3.commons.config.loadConfigs
import com.d3.commons.config.loadEthPasswords
import com.d3.eth.sidechain.util.DeployHelper
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.map
import mu.KLogging
import java.math.BigInteger

private val logger = KLogging().logger

/**
 * Task that transfer Ethereum from contract as internal transaction for testing purpose.
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        logger.error { "No arguments provided." }
        System.exit(1)
    }
    val addr = args[0]
    val amount = BigInteger.valueOf((1000000000000000000 * args[1].toDouble()).toLong())

    logger.info { "Send ether $amount from genesis to $addr" }

    loadConfigs("predeploy.ethereum", EthereumConfig::class.java, "/eth/predeploy.properties")
        .fanout { loadEthPasswords("predeploy", "/eth/ethereum_password.properties") }
        .map { (ethereumConfig, passwordConfig) ->
            DeployHelper(
                ethereumConfig,
                passwordConfig
            )
        }
        .map { deployHelper ->
            val transferEthereum = deployHelper.deployTransferEthereum()
            deployHelper.sendEthereum(amount, transferEthereum.contractAddress)

            val hash = transferEthereum.transfer(addr, amount).send().transactionHash
            logger.info { "Ether was sent, tx_hash=$hash" }
        }
        .failure { ex ->
            logger.error("Cannot send eth", ex)
            System.exit(1)
        }
}
