package algorithm

import com.nhaarman.mockito_kotlin.mock
import io.reactivex.Observable
import mu.KLogging
import sideChain.*
import sideChain.iroha.IrohaOrderedBatch

/**
 * Dummy implementation of [Notary] with effective dependencies
 */
class NotaryStub(
    private val ethHandler: ChainHandler<NotaryEvent>,
    private val irohaHandler: ChainHandler<NotaryEvent>
) : Notary {

    override fun onEthEvent(ethEvent: NotaryEvent.EthChainEvent) {
        logger.info { "Notary performs ETH event" }
    }

    override fun onIrohaEvent(irohaEvent: NotaryEvent.IrohaChainEvent) {
        logger.info { "Notary performs IROHA event" }
    }

    /**
     * Dummy implementation with relaying eth events for output
     */
    override fun irohaOutput(): Observable<IrohaOrderedBatch> {
        return io.reactivex.Observable.merge(
            ethHandler.onNewEvent(),
            irohaHandler.onNewEvent()
        ).map {
            when (it) {
                is NotaryEvent.EthChainEvent -> onEthEvent(mock<NotaryEvent.EthChainEvent.OnEthSidechainTransfer>())
                is NotaryEvent.IrohaChainEvent -> onIrohaEvent(mock<NotaryEvent.IrohaChainEvent.OnIrohaAddPeer>())
            }
            logger.info { "Notary does some work" }
            mock<IrohaOrderedBatch>()
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
