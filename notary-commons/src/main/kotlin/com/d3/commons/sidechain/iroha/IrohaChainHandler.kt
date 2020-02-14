/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.iroha

import com.d3.commons.sidechain.ChainHandler
import com.d3.commons.sidechain.SideChainEvent
import com.d3.commons.sidechain.iroha.util.getWithdrawalCommands
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
                val withdrawalCommands = getWithdrawalCommands(tx, withdrawalAccount)
                if (withdrawalCommands.isNotEmpty()) {
                    val hash = Utils.toHexHash(tx)
                    withdrawalCommands.map { command ->
                        val transferAsset = command.transferAsset
                        logger.info {
                            "transfer iroha event (from: ${transferAsset.srcAccountId}, " +
                                    "to ${transferAsset.destAccountId}, " +
                                    "amount: ${transferAsset.amount}, " +
                                    "asset: ${transferAsset.assetId}) " +
                                    "description: ${transferAsset.description}" +
                                    "txHash=$hash"
                        }
                        SideChainEvent.IrohaEvent.SideChainTransfer.fromProto(
                            transferAsset,
                            hash
                        )
                    }.toList()
                } else {
                    emptyList()
                }
            }
            .flatten()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
