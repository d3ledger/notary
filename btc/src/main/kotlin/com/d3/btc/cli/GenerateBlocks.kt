/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("BtcGenerateBlocksMain")

package com.d3.btc.cli

import com.github.jleskovar.btcrpc.BitcoinRpcClientFactory
import com.github.kittinunf.result.Result
import com.d3.commons.config.loadConfigs
import mu.KLogging

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
 * Generates blocks in Bitcoin blockchain
 * @param args - array full of arguments. args[0] - number of blocks to generate
 */
fun main(args: Array<String>) {
    val blocksToGenerate = args[0].toInt()
    Result.of {
        rpcClient.generate(blocksToGenerate)
    }.fold({
        logger.info { prettyLogMessage(blocksToGenerate) }
    }, { ex ->
        logger.error("Cannot generate blocks", ex)
    })
}

// Returns pretty log message that contains information about how many blocks were generated
private fun prettyLogMessage(blocksToGenerate: Int): String {
    return if (blocksToGenerate > 1) {
        "$blocksToGenerate blocks were successfully generated"
    } else {
        "$blocksToGenerate block was successfully generated"
    }
}
