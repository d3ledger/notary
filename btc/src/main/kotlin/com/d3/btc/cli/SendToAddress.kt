@file:JvmName("BtcSendToAddressMain")

package com.d3.btc.cli

import com.github.jleskovar.btcrpc.BitcoinRpcClientFactory
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import com.d3.commons.config.loadConfigs
import mu.KLogging
import java.math.BigDecimal

private const val CONFIRMATION_BLOCKS = 6
private val logger = KLogging().logger

private val btcNodeRpcConfig =
    loadConfigs("btc-node-rpc", BtcNodeRpcConfig::class.java, "/btc/node-rpc.properties").get()

private val rpcClient = BitcoinRpcClientFactory.createClient(
    user = btcNodeRpcConfig.user,
    password = btcNodeRpcConfig.password,
    host = btcNodeRpcConfig.host,
    port = btcNodeRpcConfig.port,
    secure = false
)

/**
 * Sends money to desired Bitcoin address
 * @param args - array full of arguments. args[0] - address, args[1] - amount of BTC to send
 */
fun main(args: Array<String>) {
    val address = args[0]
    val btcAmount = args[1]
    Result.of {
        rpcClient.sendToAddress(address, BigDecimal(btcAmount))
    }.map {
        rpcClient.generate(CONFIRMATION_BLOCKS)
    }.fold({
        logger.info { "BTC $btcAmount was successfully sent to $address" }
    }, { ex ->
        logger.error("Cannot send money", ex)
    })
}
