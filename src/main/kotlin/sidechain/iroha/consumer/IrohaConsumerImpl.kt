package sidechain.iroha.consumer

import iroha.protocol.BlockOuterClass
import jp.co.soramitsu.iroha.Keypair
import jp.co.soramitsu.iroha.ModelProtoTransaction
import jp.co.soramitsu.iroha.UnsignedTx
import mu.KLogging
import sidechain.iroha.util.toByteArray
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
        val txblob = ModelProtoTransaction(utx).signAndAddSignature(keypair).finish().blob().toByteArray()

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
