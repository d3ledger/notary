/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.iroha

import com.d3.commons.sidechain.ChainHandler
import com.d3.commons.sidechain.SideChainEvent
import com.d3.commons.sidechain.iroha.util.isWithdrawalTransaction
import com.d3.commons.util.hex
import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging

/**
 * Implementation of [ChainHandler] to convert from Iroha protocol to [SideChainEvent.IrohaEvent]
 * @param withdrawalAccount - withdrawal account in order to identify withdrawal events
 * @param feeDescription - string to identify fee transfers
 */
class IrohaChainHandler(val withdrawalAccount: String, val feeDescription: String) :
    ChainHandler<iroha.protocol.BlockOuterClass.Block> {

    /**
     * Parse Iroha block for interesting commands
     * @param block - Iroha block
     */
    override fun parseBlock(block: iroha.protocol.BlockOuterClass.Block): List<SideChainEvent.IrohaEvent> {
        logger.info { "Iroha chain handler for block ${block.blockV1.payload.height}" }

        return block.blockV1.payload.transactionsList
            .map { tx ->
                when {
                    isWithdrawalTransaction(tx, withdrawalAccount) -> {
                        val hash = String.hex(Utils.hash(tx))
                        val command = tx.payload.reducedPayload.commandsList
                            .first { cmd -> cmd.hasTransferAsset() && cmd.transferAsset.description != feeDescription }

                        logger.info {
                            "transfer iroha event (from: ${command.transferAsset.srcAccountId}, " +
                                    "to ${command.transferAsset.destAccountId}, " +
                                    "amount: ${command.transferAsset.amount}, " +
                                    "asset: ${command.transferAsset.assetId}) " +
                                    "description: ${command.transferAsset.description}" +
                                    "txHash=$hash"
                        }
                        listOf(
                            SideChainEvent.IrohaEvent.SideChainTransfer.fromProto(
                                command.transferAsset,
                                hash
                            )
                        )
                    }
                    else -> listOf()
                }
            }
            .flatten()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
