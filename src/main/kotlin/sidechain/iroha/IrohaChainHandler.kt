package sidechain.iroha

import mu.KLogging
import notary.IrohaCommand
import sidechain.ChainHandler
import sidechain.SideChainEvent

/**
 * Dummy implementation of [ChainHandler] with effective dependencies
 */
class IrohaChainHandler : ChainHandler<IrohaBlockStub> {

    /**
     * TODO Replace dummy with effective implementation
     */
    override fun parseBlock(block: IrohaBlockStub): List<SideChainEvent> {
        logger.info { "Iroha chain handler" }
        return block.transactions
            .flatMap { it.commands }
            .filter { it is IrohaCommand.CommandAddPeer }
            .map {
                it as IrohaCommand.CommandAddPeer
                SideChainEvent.IrohaEvent.OnIrohaAddPeer(it.address, it.peerKey)
            }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
