package notary

import sideChain.iroha.IrohaOrderedBatch

/**
 * Interface for performing 2WP in Iroha and side chains
 */
interface Notary {

    /**
     * Calls when Ethereum event is occurred
     */
    fun onEthEvent(ethEvent: NotaryEvent.EthChainEvent)

    /**
     * Calls when Iroha event is occurred
     */
    fun onIrohaEvent(irohaEvent: NotaryEvent.IrohaChainEvent)

    /**
     * Observable with output for sending to Iroha
     */
    fun irohaOutput(): io.reactivex.Observable<IrohaOrderedBatch>
}
