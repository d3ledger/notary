package sidechain.iroha

import mu.KLogging
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
                val bs = it.toByteArray()
                when {
                    it.hasAddPeer() ->
                        SideChainEvent.IrohaEvent.OnIrohaAddPeer(
                            SideChainEvent.IrohaEvent.OnIrohaAddPeer.fromProto(bs)
                        )

                    it.hasTransferAsset() ->
                        SideChainEvent.IrohaEvent.OnIrohaSideChainTransfer(
                            SideChainEvent.IrohaEvent.OnIrohaSideChainTransfer.fromProto(bs)
                        )


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
