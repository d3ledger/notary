package sidechain.iroha.consumer

import jp.co.soramitsu.iroha.UnsignedTx
import iroha.protocol.BlockOuterClass

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
