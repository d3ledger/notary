package algorithm

import sideChain.EthChainEvent
import sideChain.IrohaChainEvent
import sideChain.IrohaOrderedBatch

/**
 * Class conducts work of notary for performing 2WP in Iroha and side chains
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
    fun IrohaOutput() : io.reactivex.Observable<IrohaOrderedBatch>
}
