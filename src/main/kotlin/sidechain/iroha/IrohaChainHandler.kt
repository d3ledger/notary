package sidechain.iroha

import jp.co.soramitsu.iroha.iroha.hashTransaction
import mu.KLogging
import sidechain.ChainHandler
import sidechain.SideChainEvent
import sidechain.iroha.util.getHash
import sidechain.iroha.util.toByteVector

/**
 * Implementation of [ChainHandler] to convert from Iroha protocol to [SideChainEvent.IrohaEvent]
 */
class IrohaChainHandler : ChainHandler<iroha.protocol.BlockOuterClass.Block> {

    /**
     * Parse Iroha block for interesting commands
     */
    override fun parseBlock(block: iroha.protocol.BlockOuterClass.Block): List<SideChainEvent.IrohaEvent> {
        logger.info { "Iroha chain handler" }

        var hash = ""
        return block.payload.transactionsList
            .map {
                hash = hashTransaction(it.toByteArray().toByteVector()).toString()
                it
            }
            .flatMap { it.payload.reducedPayload.commandsList }
            .map {
                when {
                //TODO: create separate ChainHandler impl for withdrawal proof events
                    it.hasAddPeer() -> listOf(SideChainEvent.IrohaEvent.AddPeer.fromProto(it.addPeer))
                    it.hasTransferAsset() -> listOf(
                        SideChainEvent.IrohaEvent.SideChainTransfer.fromProto(
                            it.transferAsset,
                            hash
                        )
                    )
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
