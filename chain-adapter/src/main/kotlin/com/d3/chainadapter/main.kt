@file:JvmName("ChainAdapterMain")

package com.d3.chainadapter

import com.d3.chainadapter.adapter.ChainAdapter
import com.d3.chainadapter.provider.FileBasedLastReadBlockProvider
import com.d3.commons.config.RMQConfig
import com.d3.commons.config.getConfigFolder
import com.d3.commons.config.loadRawConfigs
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.IrohaChainListener
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.createPrettySingleThreadPool
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import io.grpc.ManagedChannelBuilder
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import mu.KLogging
import java.io.File
import java.io.IOException

private val logger = KLogging().logger
const val CHAIN_ADAPTER_SERVICE_NAME = "chain-adapter"

//TODO Springify
fun main(args: Array<String>) {
    val rmqConfig = loadRawConfigs("rmq", RMQConfig::class.java, "${getConfigFolder()}/rmq.properties")

    val irohaCredential = rmqConfig.irohaCredential
    ModelUtil.loadKeypair(irohaCredential.pubkeyPath, irohaCredential.privkeyPath).map { keyPair ->
        createLastReadBlockFile(rmqConfig)
        /**
         * It's essential to handle blocks in this service one-by-one.
         * This is why we explicitly set single threaded executor.
         */
        val irohaAPI = IrohaAPI(rmqConfig.iroha.hostname, rmqConfig.iroha.port)
        irohaAPI.setChannelForStreamingQueryStub(
            ManagedChannelBuilder.forAddress(
                rmqConfig.iroha.hostname, rmqConfig.iroha.port
            ).executor(
                createPrettySingleThreadPool(
                    CHAIN_ADAPTER_SERVICE_NAME,
                    "iroha-chain-listener"
                )
            ).usePlaintext().build()
        )
        val queryAPI =
            QueryAPI(
                irohaAPI,
                irohaCredential.accountId,
                keyPair
            )
        val irohaChainListener = IrohaChainListener(
            irohaAPI,
            IrohaCredential(irohaCredential.accountId, keyPair)
        )
        val adapter = ChainAdapter(
            rmqConfig,
            queryAPI,
            irohaChainListener,
            FileBasedLastReadBlockProvider(rmqConfig)
        )
        adapter.init { System.exit(1) }.fold(
            { logger.info("Chain adapter has been started") },
            { ex ->
                adapter.close()
                throw ex
            })
    }.failure { ex ->
        logger.error("Cannot start chain-adapter", ex)
        System.exit(1)
    }
}

/**
 * Creates last read block file
 * @param rmqConfig - RabbitMQ config
 */
private fun createLastReadBlockFile(rmqConfig: RMQConfig) {
    val file = File(rmqConfig.lastReadBlockFilePath)
    if (file.exists()) {
        //No need to create
        return
    }
    val folder = File(file.parentFile.absolutePath)
    if (!folder.exists() && !folder.mkdirs()) {
        throw IOException("Cannot create folder")
    } else if (!file.createNewFile()) {
        throw IOException("Cannot create file for last read block storage")
    }
}
