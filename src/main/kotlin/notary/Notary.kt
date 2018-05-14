package notary

/**
 * Interface for performing 2WP in Iroha and side chains
 */
interface Notary {

    /**
     * Calls when Ethereum event is occurred
     * @return transaction to be commited to Iroha
     */
    fun onEthEvent(ethEvent: NotaryEvent.EthChainEvent): IrohaOrderedBatch

    /**
     * Calls when Iroha event is occurred
     * @return transaction to be commited to Iroha
     */
    fun onIrohaEvent(irohaEvent: NotaryEvent.IrohaChainEvent): IrohaOrderedBatch

    /**
     * Observable with output for sending to Iroha
     */
    fun irohaOutput(): io.reactivex.Observable<IrohaOrderedBatch>
}
