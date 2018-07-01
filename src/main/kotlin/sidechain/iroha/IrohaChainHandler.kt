package sidechain.iroha

import iroha.protocol.Commands
import mu.KLogging
import notary.IrohaCommand
import sidechain.ChainHandler
import sidechain.SideChainEvent

/**
 * Dummy implementation of [ChainHandler] with effective dependencies
 */
class IrohaChainHandler : ChainHandler<iroha.protocol.BlockOuterClass.Block> {

    /**
     * TODO Replace dummy with effective implementation
     */
    override fun parseBlock(block: iroha.protocol.BlockOuterClass.Block): List<SideChainEvent> {
        logger.info { "Iroha chain handler" }
        return block.payload.transactionsList
            .flatMap { it.payload.commandsList }
            .map {
                when {
                    it.hasAddPeer() -> {
                        val modelCmd = IrohaCommand.CommandAddPeer.fromProto(it.toByteArray())
                        SideChainEvent.IrohaEvent.OnIrohaAddPeer(modelCmd.address, modelCmd.peerKey)
                    }
                    else -> null
                }
            }
            .filterNotNull()

    }

    /**
     * Logger
     */
    companion object : KLogging()
}
