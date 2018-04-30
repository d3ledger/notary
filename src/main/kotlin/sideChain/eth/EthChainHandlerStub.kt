package sideChain.eth

import com.nhaarman.mockito_kotlin.mock
import io.reactivex.Observable
import mu.KLogging
import sideChain.ChainHandler
import sideChain.ChainListener

/**
 * Dummy implementation of [ChainHandler] with effective dependencies
 */
class EthChainHandlerStub constructor(private val listenerStub: ChainListener<EthBlockStub>) :
    ChainHandler<EthChainEvent> {

    override fun onNewEvent(): Observable<EthChainEvent> {
        return listenerStub.onNewBlockObservable().map {
            logger.info { "Eth chain handler" }
            mock<OnEthSidechainTransfer>()
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
