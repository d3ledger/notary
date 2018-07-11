package sidechain.iroha

import mu.KLogging
import sidechain.ChainHandler
import sidechain.SideChainEvent

/**
 * Implementation of [ChainHandler] to convert from Iroha protocol to [SideChainEvent.IrohaEvent]
 */
class IrohaChainHandler : ChainHandler<iroha.protocol.BlockOuterClass.Block> {

    /**
     * Parse Iroha block for interesting commands
     */
    override fun parseBlock(block: iroha.protocol.BlockOuterClass.Block): List<SideChainEvent.IrohaEvent> {
        logger.info { "Iroha chain handler" }
        return block.payload.transactionsList
            .flatMap { it.payload.reducedPayload.commandsList }
            .flatMap {
                when {
                    it.hasAddPeer() -> listOf(SideChainEvent.IrohaEvent.AddPeer.fromProto(it.addPeer))
                    it.hasTransferAsset() -> listOf(SideChainEvent.IrohaEvent.SideChainTransfer.fromProto(it.transferAsset))
                    else -> listOf()
                }
            }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
