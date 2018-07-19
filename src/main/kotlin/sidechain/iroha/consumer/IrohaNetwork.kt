package sidechain.iroha.consumer

import iroha.protocol.TransactionOuterClass

/**
 * Interface for network layer of Iroha chain
 */
interface IrohaNetwork {
    /**
     * Send transaction to Iroha
     * @param protoTx protobuf representation of transaction
     */
    fun send(protoTx: TransactionOuterClass.Transaction)
}
