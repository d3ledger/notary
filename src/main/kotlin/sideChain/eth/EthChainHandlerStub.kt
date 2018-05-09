package sideChain.eth

import notary.NotaryEvent
import com.nhaarman.mockito_kotlin.mock
import io.reactivex.Observable
import mu.KLogging
import org.web3j.protocol.core.methods.response.EthBlock
import sideChain.ChainHandler
import sideChain.ChainListener

/**
 * Dummy implementation of [ChainHandler] with effective dependencies
 */
class EthChainHandlerStub constructor(private val listenerStub: ChainListener<EthBlock>) :
    ChainHandler {

    override fun onNewEvent(): Observable<NotaryEvent> {
        return listenerStub.onNewBlockObservable().map {
            logger.info { "Eth chain handler" }
            println("handler got block #${it.block.number}")
            mock<NotaryEvent.EthChainEvent.OnEthSidechainTransfer>()
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
