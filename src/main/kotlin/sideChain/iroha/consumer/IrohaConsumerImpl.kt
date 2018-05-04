package sideChain.iroha.consumer

import ByteVector
import ModelProtoTransaction
import ModelTransactionBuilder
import PublicKey
import iroha.protocol.BlockOuterClass
import mu.KLogging
import notary.IrohaCommand
import notary.IrohaOrderedBatch
import notary.IrohaTransaction
import notary.Notary
import java.math.BigInteger
import kotlin.reflect.jvm.internal.impl.protobuf.InvalidProtocolBufferException

/**
 * Endpoint of Iroha to write transactions
 * @param source that emits [IrohaCommand]s
 * @param cryptoProvider provides with Iroha keypair
 * @param irohaNetwork network layer of Iroha
 */
class IrohaConsumerImpl(
    source: io.reactivex.Observable<IrohaOrderedBatch>,
    cryptoProvider: IrohaCryptoProvider, val irohaNetwork: IrohaNetworkImpl
) : IrohaConsumer {

    /** Iroha keypair for signing */
    val keypair = cryptoProvider.keypair

    init {
        source.subscribe {
            irohaOutput(it)
        }
    }

    /** Convert [ByteVector] to [ByteArray] */
    private fun toByteArray(blob: ByteVector): ByteArray {
        val size = blob.size().toInt()
        val bs = ByteArray(size)
        for (i in 0 until size) {
            bs[i] = blob.get(i).toByte()
        }
        return bs
    }

    /** Convert transactions from [Notary] model to protobuf object to be sent to Iroha */
    fun convert(transaction: IrohaTransaction): BlockOuterClass.Transaction {
        var txBuilder = ModelTransactionBuilder()
            .creatorAccountId(transaction.creator)
            .createdTime(BigInteger.valueOf(System.currentTimeMillis()))

        for (cmd in transaction.commands) {
            when (cmd) {
                is IrohaCommand.CommandAddAssetQuantity ->
                    txBuilder = txBuilder.addAssetQuantity(
                        cmd.accountId,
                        cmd.assetId,
                        cmd.amount
                    )
                is IrohaCommand.CommandAddSignatory ->
                    txBuilder = txBuilder.addSignatory(
                        cmd.accountId,
                        PublicKey(cmd.publicKey)
                    )
                is IrohaCommand.CommandCreateAsset ->
                    txBuilder = txBuilder.createAsset(
                        cmd.assetName,
                        cmd.domainId,
                        cmd.precision
                    )
                is IrohaCommand.CommandSetAccountDetail ->
                    txBuilder = txBuilder.setAccountDetail(
                        cmd.accountId,
                        cmd.key,
                        cmd.value
                    )
                is IrohaCommand.CommandTransferAsset ->
                    txBuilder = txBuilder.transferAsset(
                        cmd.srcAccountId,
                        cmd.destAccountId,
                        cmd.assetId,
                        cmd.description,
                        cmd.amount
                    )
            }
        }
        val utx = txBuilder.build()

        // sign transaction and get its binary representation (Blob)
        val txblob = ModelProtoTransaction().signAndAddSignature(utx, keypair).blob()

        // Convert ByteVector to byte array
        val bs = toByteArray(txblob)

        // create proto object
        lateinit var protoTx: BlockOuterClass.Transaction
        try {
            protoTx = BlockOuterClass.Transaction.parseFrom(bs)
        } catch (e: InvalidProtocolBufferException) {
            logger.error { "Exception while converting byte array to protobuf: ${e.message}" }
            System.exit(1)
        }
        return protoTx
    }

    /** Callback to be called to write to Iroha */
    override fun irohaOutput(batch: IrohaOrderedBatch) {
        IrohaNetworkImpl.logger.info { "TX to IROHA" }

        // TODO rework with batch transactions
        for (tx in batch.transactions) {
            val protoTx = convert(tx)

            // Send transaction to iroha
            irohaNetwork.send(protoTx)

        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
