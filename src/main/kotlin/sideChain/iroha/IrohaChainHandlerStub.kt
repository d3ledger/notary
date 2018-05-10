package sideChain.iroha

import notary.NotaryEvent
import com.nhaarman.mockito_kotlin.mock
import io.reactivex.Observable
import mu.KLogging
import sideChain.ChainHandler
import sideChain.ChainListener

/**
 * Dummy implementation of [ChainHandler] with effective dependencies
 */
class IrohaChainHandlerStub : ChainHandler<IrohaBlockStub> {

    /**
     * TODO Replace dummy with effective implementation
     */
    override fun parseBlock(block: IrohaBlockStub): NotaryEvent {
        logger.info { "Iroha chain handler" }
        return mock<NotaryEvent.IrohaChainEvent.OnIrohaAddPeer>()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
