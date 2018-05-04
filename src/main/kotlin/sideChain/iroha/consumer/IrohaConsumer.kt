package sideChain.iroha.consumer

import notary.IrohaOrderedBatch

/**
 * Interface for consuming Iroha events provided by [notary.Notary]
 */
interface IrohaConsumer {

    /**
     * called on new event from [notary.Notary]
     */
    fun irohaOutput(batch: IrohaOrderedBatch)
}
