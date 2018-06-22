package notary

import sidechain.SideChainEvent

/**
 * Interface for performing 2WP in Iroha and side chains
 */
interface Notary {

    /**
     * Calls when Ethereum event is occurred
     * @return transaction to be commited to Iroha
     */
    fun onEthEvent(ethInputEvent: SideChainEvent.EthereumEvent): IrohaOrderedBatch

    /**
     * Calls when Iroha event is occurred
     * @return transaction to be commited to Iroha
     */
    fun onIrohaEvent(irohaInputEvent: SideChainEvent.IrohaEvent): IrohaOrderedBatch

    /**
     * Observable with output for sending to Iroha
     */
    fun irohaOutput(): io.reactivex.Observable<IrohaOrderedBatch>
}
