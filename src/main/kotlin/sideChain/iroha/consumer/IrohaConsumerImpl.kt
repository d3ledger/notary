package sideChain.iroha.consumer

import Keypair
import ModelProtoTransaction
import UnsignedTx
import iroha.protocol.BlockOuterClass
import mu.KLogging
import sideChain.iroha.util.toByteArray
import kotlin.reflect.jvm.internal.impl.protobuf.InvalidProtocolBufferException

/**
 * Endpoint of Iroha to write transactions
 * @param keypair Iroha keypair for signing
 */
class IrohaConsumerImpl(val keypair: Keypair) : IrohaConsumer {

    /** Convert transaction to Protobuf
     * @param utx unsigned Iroha transaction
     */
    override fun convertToProto(utx: UnsignedTx): BlockOuterClass.Transaction {
        // sign transaction and get its binary representation (Blob)
        val txblob = ModelProtoTransaction().signAndAddSignature(utx, keypair).blob().toByteArray()

        // create proto object
        lateinit var protoTx: BlockOuterClass.Transaction
        try {
            protoTx = BlockOuterClass.Transaction.parseFrom(txblob)
        } catch (e: InvalidProtocolBufferException) {
            logger.error { "Exception while converting byte array to protobuf: ${e.message}" }
            System.exit(1)
        }
        return protoTx
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
