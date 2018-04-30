package sideChain.iroha

import algorithm.NotaryEvent
import com.nhaarman.mockito_kotlin.mock
import io.reactivex.Observable
import mu.KLogging
import sideChain.ChainHandler
import sideChain.ChainListener

/**
 * Dummy implementation of [ChainHandler] with effective dependencies
 */
class IrohaChainHandlerStub constructor(private val listenerStub: ChainListener<IrohaBlockStub>) :
    ChainHandler<NotaryEvent> {

    override fun onNewEvent(): Observable<NotaryEvent> {
        return listenerStub.onNewBlockObservable().map {
            logger.info { "Iroha chain handler" }
            mock<NotaryEvent.IrohaChainEvent.OnIrohaAddPeer>()
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
