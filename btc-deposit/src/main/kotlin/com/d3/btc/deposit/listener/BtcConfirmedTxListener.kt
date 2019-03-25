/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.btc.deposit.listener

import mu.KLogging
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionConfidence
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Listener of Bitcoin transaction block depth change
 * @param confidenceLevel - depth of block to wait until calling [txHandler]
 * @param tx - transaction to listen
 * @param blockTime - time of block where [tx] appeared for the first time. This time is used in MST
 * @param txHandler - function that is called when [tx] hits [confidenceLevel] depth in Blockchain
 */
class BtcConfirmedTxListener(
    private val confidenceLevel: Int,
    private val tx: Transaction,
    private val blockTime: Date,
    private val txHandler: (Transaction, Date) -> Unit
) : TransactionConfidence.Listener {
    private val processed = AtomicBoolean()

    init {
        logger.info("Listener for ${tx.hashAsString} has been created. Block time is $blockTime")
    }

    override fun onConfidenceChanged(
        confidence: TransactionConfidence,
        reason: TransactionConfidence.Listener.ChangeReason
    ) {
        /*
        Due to bitoinj library threading issues, we can miss an event of 'depthInBlocks'
        being exactly 'confidenceLevel'. So we check it to be at least 'confidenceLevel'.
        This leads D3 to handle the same transaction many times. This is why we use a special
        flag to check if it has been handled already.
        */
        val currentDepth = confidence.depthInBlocks
        if (currentDepth >= confidenceLevel
            && processed.compareAndSet(false, true)
        ) {
            logger.info { "BTC tx ${tx.hashAsString} was confirmed" }
            confidence.removeEventListener(this)
            txHandler(tx, blockTime)
        }
        logger.info { "BTC tx ${tx.hashAsString} has $currentDepth confirmations" }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
