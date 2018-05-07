package sideChain.iroha.consumer

import Blob
import Keypair
import ModelProtoTransaction
import iroha.protocol.BlockOuterClass
import mu.KLogging
import notary.IrohaCommand
import notary.IrohaOrderedBatch
import sideChain.iroha.util.toByteArray
import kotlin.reflect.jvm.internal.impl.protobuf.InvalidProtocolBufferException

/**
 * Endpoint of Iroha to write transactions
 * @param source that emits [IrohaCommand]s
 * @param keypair Iroha keypair for signing
 * @param irohaNetwork network layer of Iroha
 */
class IrohaConsumerImpl(
    source: io.reactivex.Observable<IrohaOrderedBatch>,
    val keypair: Keypair, val irohaNetwork: IrohaNetwork
) : IrohaConsumer {

    /** Converts Notary model to Iroha Model */
    val irohaConverter = IrohaConverterImpl()

    init {
        source.subscribe {
            irohaOutput(it)
        }
    }

    /** Convert transaction to Protobuf */
    fun convertToProto(txblob: ByteArray): BlockOuterClass.Transaction {
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

    /** Callback to be called to write to Iroha */
    override fun irohaOutput(batch: IrohaOrderedBatch) {
        // TODO rework with batch transactions
        for (tx in batch.transactions) {
            val utx = irohaConverter.convert(tx)

            // sign transaction and get its binary representation (Blob)
            val txblob = ModelProtoTransaction().signAndAddSignature(utx, keypair).blob().toByteArray()

            val protoTx = convertToProto(txblob)

            // Send transaction to iroha
            irohaNetwork.send(protoTx)
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
