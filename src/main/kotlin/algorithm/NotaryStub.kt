package algorithm

import com.nhaarman.mockito_kotlin.mock
import io.reactivex.Observable
import mu.KLogging
import sideChain.*
import sideChain.eth.EthChainEvent
import sideChain.eth.OnEthSidechainTransfer

/**
 * Dummy implementation of [Notary] with effective dependencies
 */
class NotaryStub(private val ethHandler: ChainHandler<EthChainEvent>) : Notary {

    override fun onEthEvent(ethEvent: EthChainEvent) {
        logger.info { "Notary performs ETH event" }
    }

    override fun onIrohaEvent(irohaEvent: IrohaChainEvent) {
        logger.info { "Notary performs IROHA event" }
    }

    /**
     * Dummy implementation with relaying eth events for output
     */
    override fun irohaOutput(): Observable<IrohaOrderedBatch> {
        return ethHandler.onNewEvent().map {
            onEthEvent(mock<OnEthSidechainTransfer>())
            logger.info { "Notary make some work" }
            mock<IrohaOrderedBatch>()
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
