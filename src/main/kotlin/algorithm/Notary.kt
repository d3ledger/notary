package algorithm

import sideChain.eth.EthChainEvent
import sideChain.IrohaChainEvent
import sideChain.IrohaOrderedBatch

/**
 * Interface for performing 2WP in Iroha and side chains
 */
interface Notary {

    /**
     * Calls when Ethereum event is occurred
     */
    fun onEthEvent(ethEvent: EthChainEvent)

    /**
     * Calls when Iroha event is occurred
     */
    fun onIrohaEvent(irohaEvent: IrohaChainEvent)

    /**
     * Observable with output for sending to Iroha
     */
    fun irohaOutput(): io.reactivex.Observable<IrohaOrderedBatch>
}
