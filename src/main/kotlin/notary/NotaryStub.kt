package notary

import com.nhaarman.mockito_kotlin.mock
import io.reactivex.Observable
import mu.KLogging
import sideChain.ChainHandler

/**
 * Dummy implementation of [Notary] with effective dependencies
 */
class NotaryStub(
    private val ethHandler: ChainHandler,
    private val irohaHandler: ChainHandler
) : Notary {

    /**
     * Handle Ehthereum event
     */
    override fun onEthEvent(ethEvent: NotaryEvent.EthChainEvent) {
        logger.info { "Notary performs ETH event" }
    }

    /**
     * Handle Iroha event
     */
    override fun onIrohaEvent(irohaEvent: NotaryEvent.IrohaChainEvent) {
        logger.info { "Notary performs IROHA event" }
    }

    /**
     * Relay side chain [NotaryEvent] to Iroha output
     */
    override fun irohaOutput(): Observable<IrohaOrderedBatch> {
        // TODO move business logic away from here
        return io.reactivex.Observable.merge(
            ethHandler.onNewEvent(),
            irohaHandler.onNewEvent()
        ).map {
            when (it) {
                is NotaryEvent.EthChainEvent -> onEthEvent(mock<NotaryEvent.EthChainEvent.OnEthSidechainTransfer>())
                is NotaryEvent.IrohaChainEvent -> onIrohaEvent(mock<NotaryEvent.IrohaChainEvent.OnIrohaAddPeer>())
            }
            logger.info { "Notary does some work" }

            // TODO replace output with effective implementation
            IrohaOrderedBatch(arrayListOf())
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
