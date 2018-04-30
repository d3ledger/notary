package sideChain.eth

import notary.NotaryEvent
import com.nhaarman.mockito_kotlin.mock
import io.reactivex.Observable
import mu.KLogging
import sideChain.ChainHandler
import sideChain.ChainListener

/**
 * Dummy implementation of [ChainHandler] with effective dependencies
 */
class EthChainHandlerStub constructor(private val listenerStub: ChainListener<EthBlockStub>) :
    ChainHandler {

    override fun onNewEvent(): Observable<NotaryEvent> {
        return listenerStub.onNewBlockObservable().map {
            logger.info { "Eth chain handler" }
            mock<NotaryEvent.EthChainEvent.OnEthSidechainTransfer>()
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
