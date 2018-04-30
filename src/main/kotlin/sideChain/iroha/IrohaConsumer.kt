package sideChain.iroha

/**
 * Interface for consuming Iroha events provided by [Notary]
 */
interface IrohaConsumer {

    /**
     * called on new event from [Notary]
     */
    fun onIrohaEvent(batch: IrohaOrderedBatch)
}
