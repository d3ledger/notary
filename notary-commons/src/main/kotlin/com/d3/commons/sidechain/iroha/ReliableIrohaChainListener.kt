/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.iroha

import com.d3.commons.config.RMQConfig
import com.d3.commons.sidechain.ChainListener
import com.github.kittinunf.result.Result
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Delivery
import com.rabbitmq.client.GetResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import iroha.protocol.BlockOuterClass
import mu.KLogging
import java.util.concurrent.ExecutorService

private const val DEFAULT_LAST_READ_BLOCK = -1L

/**
 * Rabbit MQ based implementation of [ChainListener]
 * @param rmqConfig - Rabbit MQ configuration
 * @param irohaQueue - name of queue to read Iroha blocks from
 * @param subscribe - function that will be called on new block
 * @param consumerExecutorService - executor that is used to execure RabbitMQ consumer code.
 * @param autoAck - enables auto acknowledgment
 */
class ReliableIrohaChainListener(
    private val rmqConfig: RMQConfig,
    private val irohaQueue: String,
    private val subscribe: (iroha.protocol.BlockOuterClass.Block, () -> Unit) -> Unit,
    private val consumerExecutorService: ExecutorService?,
    private val autoAck: Boolean
) : ChainListener<Pair<iroha.protocol.BlockOuterClass.Block, () -> Unit>> {
    constructor(
        rmqConfig: RMQConfig,
        irohaQueue: String
    ) : this(rmqConfig, irohaQueue, { _, _ -> }, null, true)

    constructor(
        rmqConfig: RMQConfig,
        irohaQueue: String,
        consumerExecutorService: ExecutorService
    ) : this(rmqConfig, irohaQueue, { _, _ -> }, consumerExecutorService, true)


    private val factory = ConnectionFactory()

    // Last read Iroha block number. Used to detect double read.
    private var lastReadBlockNum: Long = DEFAULT_LAST_READ_BLOCK

    private val conn by lazy {
        factory.host = rmqConfig.host
        if (consumerExecutorService != null) {
            factory.newConnection(consumerExecutorService)
        } else {
            factory.newConnection()
        }

    }

    private val channel by lazy { conn.createChannel() }

    private var consumerTag: String? = null

    init {
        channel.exchangeDeclare(rmqConfig.irohaExchange, "fanout", true)
        channel.queueDeclare(irohaQueue, true, false, false, null)
        channel.queueBind(irohaQueue, rmqConfig.irohaExchange, "")
        channel.basicQos(1)
    }

    /**
     * Returns an observable that emits a new block every time it gets it from Iroha
     */
    override fun getBlockObservable(
    ): Result<Observable<Pair<BlockOuterClass.Block, () -> Unit>>, Exception> {
        return Result.of {
            val source = PublishSubject.create<Pair<BlockOuterClass.Block, Long>>()
            val deliverCallback = { consumerTag: String, delivery: Delivery ->
                // This code is executed inside consumerExecutorService
                val block = iroha.protocol.BlockOuterClass.Block.parseFrom(delivery.body)
                //TODO shall we ignore too old blocks?
                if (ableToHandleBlock(block)) {
                    source.onNext(Pair(block, delivery.envelope.deliveryTag))
                } else {
                    logger.warn { "Not able to handle Iroha block ${block.blockV1.payload.height}" }
                    if (!autoAck) {
                        confirmDelivery(delivery.envelope.deliveryTag)
                    }
                }
            }
            val obs: Observable<Pair<iroha.protocol.BlockOuterClass.Block, () -> Unit>> =
                source.map { (block, deliveryTag) ->
                    logger.info { "New Iroha block from RMQ arrived. Height ${block.blockV1.payload.height}" }
                    Pair(block, {
                        if (!autoAck) {
                            confirmDelivery(deliveryTag)
                        }
                    })
                }
            obs.subscribe { (block, ack) -> subscribe(block, ack) }
            consumerTag = channel.basicConsume(irohaQueue, autoAck, deliverCallback, { _ -> })
            obs
        }
    }

    /**
     * @return a block as soon as it is committed to iroha
     */
    override suspend fun getBlock(): Pair<iroha.protocol.BlockOuterClass.Block, () -> Unit> {
        var resp: GetResponse?
        do {
            Thread.sleep(10L)
            resp = channel.basicGet(irohaQueue, autoAck)
        } while (resp == null)

        val block = iroha.protocol.BlockOuterClass.Block.parseFrom(resp.body)
        return Pair(block, {
            if (!autoAck)
                confirmDelivery(resp.envelope.deliveryTag)
        })
    }

    /**
     * Checks if we are able to handle Iroha block
     * @param block - Iroha block to check
     * @return true if able
     */
    @Synchronized
    private fun ableToHandleBlock(block: BlockOuterClass.Block): Boolean {
        val height = block.blockV1.payload.height
        if (lastReadBlockNum == DEFAULT_LAST_READ_BLOCK) {
            //This is the very first block
            lastReadBlockNum = height
            return true
        } else if (height <= lastReadBlockNum) {
            logger.warn("Iroha block $height has been read previously")
            return false
        }
        val missedBlocks = height - lastReadBlockNum
        if (missedBlocks > 1) {
            logger.warn("Missed Iroha blocks $missedBlocks")
        }
        lastReadBlockNum = height
        return true
    }

    /**
     * Confirms Iroha block delivery
     * @param deliveryTag - delivery to confirm
     */
    private fun confirmDelivery(deliveryTag: Long) {
        channel.basicAck(deliveryTag, false)
        logger.info { "Iroha block delivery confirmed" }
    }

    fun purge() {
        channel.queuePurge(irohaQueue)
    }

    override fun close() {
        consumerTag?.let {
            channel.basicCancel(it)
        }
        consumerExecutorService?.shutdownNow()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
