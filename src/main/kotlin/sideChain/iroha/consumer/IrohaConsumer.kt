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
    fun convertToProto(utx: UnsignedTx): BlockOuterClass.Transaction
}
