package com.d3.commons.sidechain.iroha

import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging
import com.d3.commons.sidechain.ChainHandler
import com.d3.commons.sidechain.SideChainEvent
import com.d3.commons.util.hex

/**
 * Implementation of [ChainHandler] to convert from Iroha protocol to [SideChainEvent.IrohaEvent]
 */
class IrohaChainHandler : ChainHandler<iroha.protocol.BlockOuterClass.Block> {

    /**
     * Parse Iroha block for interesting commands
     */
    override fun parseBlock(block: iroha.protocol.BlockOuterClass.Block): List<SideChainEvent.IrohaEvent> {
        logger.info { "Iroha chain handler for block ${block.blockV1.payload.height}" }

        var hash = ""
        return block.blockV1.payload.transactionsList
            .map { tx ->
                hash = String.hex(Utils.hash(tx))
                tx
            }
            .flatMap { tx -> tx.payload.reducedPayload.commandsList }
            .map { command ->
                when {
                    //TODO: create separate ChainHandler impl for withdrawal proof events
                    command.hasAddPeer() -> listOf(SideChainEvent.IrohaEvent.AddPeer.fromProto(command.addPeer))
                    command.hasTransferAsset() -> {
                        logger.info { "transfer iroha event (from: ${command.transferAsset.srcAccountId}, to ${command.transferAsset.destAccountId}, amount: ${command.transferAsset.amount}, asset: ${command.transferAsset.assetId}" }
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
