package sidechain.iroha.consumer

import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.UnsignedTx

/**
 * Interface for consuming Iroha events provided by [notary.Notary]
 */
interface IrohaConsumer {

    /**
     * Sign and convert to protobuf
     * @param utx - unsigned transaction
     */
    fun convertToProto(utx: UnsignedTx): TransactionOuterClass.Transaction
}
