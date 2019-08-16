/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.exchange.exchanger.service

import com.d3.chainadapter.client.ReliableIrohaChainListener
import com.d3.exchange.exchanger.context.ExchangerContext
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import mu.KLogging
import org.springframework.stereotype.Component
import java.io.Closeable

/**
 * Class responsible for Iroha block processing and proper reacting to incoming transfers
 * in order to support automatic asset conversions
 */
@Component
class ExchangerService(
    private val chainListener: ReliableIrohaChainListener,
    private val contexts: List<ExchangerContext>
) : Closeable {

    /**
     * Starts blocks listening and processing
     */
    fun start(): Result<Unit, Exception> {
        logger.info { "Exchanger service is started. Waiting for incoming transactions." }
        return chainListener.getBlockObservable().map { observable ->
            observable.subscribe { (block, _) ->
                contexts.forEach { context ->
                    context.performConversions(block)
                }
            }
        }.flatMap {
            chainListener.listen()

        }
    }

    override fun close() {
        chainListener.close()
    }

    companion object : KLogging()
}
