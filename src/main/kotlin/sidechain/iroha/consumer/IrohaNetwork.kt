package sidechain.iroha.consumer

import iroha.protocol.BlockOuterClass

/**
 * Interface for network layer of Iroha chain
 */
interface IrohaNetwork {
    /**
     * Send transaction to Iroha
     * @param protoTx protobuf representation of transaction
     */
    fun send(protoTx: BlockOuterClass.Transaction)
}
