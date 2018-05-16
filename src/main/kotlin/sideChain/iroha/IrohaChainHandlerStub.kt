package sideChain.iroha

import notary.NotaryInputEvent
import com.nhaarman.mockito_kotlin.mock
import mu.KLogging
import sideChain.ChainHandler

/**
 * Dummy implementation of [ChainHandler] with effective dependencies
 */
class IrohaChainHandlerStub : ChainHandler<IrohaBlockStub> {

    /**
     * TODO Replace dummy with effective implementation
     */
    override fun parseBlock(block: IrohaBlockStub): List<NotaryInputEvent> {
        logger.info { "Iroha chain handler" }
        return listOf(mock<NotaryInputEvent.IrohaChainInputEvent.OnIrohaAddPeer>())
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
