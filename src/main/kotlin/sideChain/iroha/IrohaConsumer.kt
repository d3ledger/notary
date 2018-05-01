package sideChain.iroha

import notary.IrohaOrderedBatch

/**
 * Interface for consuming Iroha events provided by [notary.Notary]
 */
interface IrohaConsumer {

    /**
     * called on new event from [notary.Notary]
     */
    fun onIrohaEvent(batch: IrohaOrderedBatch)
}
