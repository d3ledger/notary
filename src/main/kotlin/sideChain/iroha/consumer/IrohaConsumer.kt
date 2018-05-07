package sideChain.iroha.consumer

import iroha.protocol.BlockOuterClass
import notary.IrohaOrderedBatch
import UnsignedTx

/**
 * Interface for consuming Iroha events provided by [notary.Notary]
 */
interface IrohaConsumer {

    /**
     * Sign and convert to protobuf
     * @param utx - unsigned transaction
     */
    fun toProto(utx: UnsignedTx): BlockOuterClass.Transaction
}
