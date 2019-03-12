package com.d3.chainadapter.adapter

import com.d3.chainadapter.CHAIN_ADAPTER_SERVICE_NAME
import com.d3.chainadapter.provider.LastReadBlockProvider
import com.d3.commons.config.RMQConfig
import com.d3.commons.sidechain.iroha.IrohaChainListener
import com.d3.commons.sidechain.iroha.util.getBlockRawResponse
import com.d3.commons.sidechain.iroha.util.getErrorMessage
import com.d3.commons.util.createPrettySingleThreadPool
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import io.reactivex.schedulers.Schedulers
import iroha.protocol.BlockOuterClass
import iroha.protocol.QryResponses
import jp.co.soramitsu.iroha.java.QueryAPI
import mu.KLogging
import java.io.Closeable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong

private const val BAD_IROHA_BLOCK_HEIGHT_ERROR_CODE = 3

/**
 * Chain adapter service
 * It reads Iroha blocks and sends them to recipients via RabbitMQ
 */
class ChainAdapter(
    private val rmqConfig: RMQConfig,
    private val queryAPI: QueryAPI,
    private val irohaChainListener: IrohaChainListener,
    private val lastReadBlockProvider: LastReadBlockProvider
) : Closeable {

    private val connectionFactory = ConnectionFactory()
    private val lastReadBlock = AtomicLong()
    private val publishUnreadLatch = CountDownLatch(1)
    private val subscriberExecutorService = createPrettySingleThreadPool(
        CHAIN_ADAPTER_SERVICE_NAME, "iroha-chain-subscriber"
    )

    init {
        connectionFactory.host = rmqConfig.host
    }

    private val connection = connectionFactory.newConnection()

    /**
     * Initiates and runs chain adapter
     */
    fun init(): Result<Unit, Exception> {
        return init({})
    }

    /**
     * Initiates and runs chain adapter
     * @param onIrohaListenError - function that will be called on Iroha chain listener error
     */
    fun init(onIrohaListenError: () -> Unit): Result<Unit, Exception> {
        return Result.of {
            lastReadBlock.set(lastReadBlockProvider.getLastBlockHeight())
            val channel = connection.createChannel()
            channel.exchangeDeclare(rmqConfig.irohaExchange, "fanout", true)
            logger.info { "Listening Iroha blocks" }
            initIrohaChainListener(channel, onIrohaListenError)
            publishUnreadIrohaBlocks(channel)
        }
    }

    /**
     * Initiates Iroha chain listener logic
     * @param channel - channel that is used to publish Iroha blocks
     * @param onIrohaListenError - function that will be called on Iroha chain listener error
     */
    private fun initIrohaChainListener(channel: Channel, onIrohaListenError: () -> Unit) {
        irohaChainListener.getBlockObservable()
            .map { observable ->
                observable.subscribeOn(Schedulers.from(subscriberExecutorService))
                    .subscribe({ block ->
                        publishUnreadLatch.await()
                        // Send only not read Iroha blocks
                        if (block.blockV1.payload.height > lastReadBlock.get()) {
                            onNewBlock(channel, block)
                        }
                    }, { ex ->
                        logger.error("Error on Iroha chain listener occurred", ex)
                        onIrohaListenError()
                    })
            }
    }

    /**
     * Publishes unread blocks
     * @param channel - RabbitMQ channel that is used to publish Iroha blocks
     */
    private fun publishUnreadIrohaBlocks(channel: Channel) {
        var lastProcessedBlock = lastReadBlockProvider.getLastBlockHeight()
        while (true) {
            lastProcessedBlock++
            logger.info { "Try read Iroha block $lastProcessedBlock" }
            val response = getBlockRawResponse(queryAPI, lastProcessedBlock)
            if (response.hasErrorResponse()) {
                val errorResponse = response.errorResponse
                if (isNoMoreBlocksError(errorResponse)) {
                    logger.info { "Done publishing unread blocks" }
                    break
                } else {
                    throw Exception("Cannot get block. ${getErrorMessage(errorResponse)}")
                }
            } else if (response.hasBlockResponse()) {
                onNewBlock(channel, response.blockResponse!!.block)
            }
        }
        publishUnreadLatch.countDown()
    }

    /**
     * Checks if no more blocks
     * @param errorResponse - error response to check
     * @return true if no more blocks to read
     */
    private fun isNoMoreBlocksError(errorResponse: QryResponses.ErrorResponse) =
        errorResponse.errorCode == BAD_IROHA_BLOCK_HEIGHT_ERROR_CODE

    /**
     * Publishes new block to RabbitMQ
     * @param channel - channel that is used to publish blocks
     * @param block - Iroha block to publish
     */
    private fun onNewBlock(channel: Channel, block: BlockOuterClass.Block) {
        val message = block.toByteArray()
        channel.basicPublish(
            rmqConfig.irohaExchange,
            "",
            MessageProperties.MINIMAL_PERSISTENT_BASIC,
            message
        )
        logger.info { "Block pushed" }
        val height = block.blockV1.payload.height
        // Save last read block
        lastReadBlockProvider.saveLastBlockHeight(height)
        lastReadBlock.set(height)
    }

    /**
     * Returns height of last read Iroha block
     */
    fun getLastReadBlock() = lastReadBlock.get()

    override fun close() {
        subscriberExecutorService.shutdownNow()
        queryAPI.api.close()
        irohaChainListener.close()
        connection.close()
    }

    companion object : KLogging()
}
