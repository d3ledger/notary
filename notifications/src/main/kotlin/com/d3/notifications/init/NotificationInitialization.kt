/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.init

import com.d3.chainadapter.client.ReliableIrohaChainListener
import com.d3.notifications.handler.CommandHandler
import com.d3.notifications.handler.IrohaCommand
import com.d3.notifications.queue.EventsQueue
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import iroha.protocol.BlockOuterClass
import mu.KLogging
import org.springframework.stereotype.Component

const val ETH_WALLET = "ethereum_wallet"

/**
 * Notifications initialization service
 */
@Component
class NotificationInitialization(
    private val irohaChainListener: ReliableIrohaChainListener,
    private val eventsQueue: EventsQueue,
    private val commandHandlers: List<CommandHandler>
) {

    /**
     * Initiates notification service
     * @param onIrohaChainFailure - function that will be called in case of Iroha failure. Does nothing by default.
     */
    fun init(onIrohaChainFailure: () -> Unit = {}): Result<Unit, Exception> {
        return irohaChainListener.getBlockObservable().map { irohaObservable ->
            irohaObservable
                .subscribe(
                    { (block, ack) ->
                        try {
                            handleBlock(block)
                        } catch (e: Exception) {
                            logger.error("Cannot handle block $block", e)
                        } finally {
                            ack()
                        }
                    }, { ex ->
                        logger.error("Error on Iroha subscribe", ex)
                        onIrohaChainFailure()
                    })
        }.flatMap { irohaChainListener.listen() }.map { eventsQueue.listen() }
    }

    /**
     * Handles given block
     * @param block - block to handle
     */
    private fun handleBlock(block: BlockOuterClass.Block) {
        block.blockV1.payload.transactionsList.forEach { tx ->
            tx.payload.reducedPayload.commandsList.forEach { command ->
                val commandWithTx = IrohaCommand(command, tx, block)
                commandHandlers.forEach { it.handleCommand(commandWithTx) }
            }
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
