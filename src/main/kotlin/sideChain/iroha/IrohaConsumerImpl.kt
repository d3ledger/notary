package sideChain.iroha

import notary.IrohaOrderedBatch
import mu.KLogging
import notary.Notary
import main.Configs
import notary.IrohaCommand
import java.nio.file.Paths
import java.math.BigInteger

// iroha
import ByteVector
import ModelCrypto
import ModelTransactionBuilder
import ModelProtoTransaction
import Keypair
import PublicKey
import kotlin.reflect.jvm.internal.impl.protobuf.InvalidProtocolBufferException
import iroha.protocol.BlockOuterClass
import iroha.protocol.CommandServiceGrpc

import io.grpc.ManagedChannelBuilder

/**
 * Dummy implementation of [IrohaConsumer] with effective dependency
 */
class IrohaConsumerImpl(notary: Notary) : IrohaConsumer {

    /** Iroha keypair */
    val keypair: Keypair

    init {
        notary.irohaOutput().subscribe {
            onIrohaEvent(it)
        }
        val crypto = ModelCrypto()
        keypair = crypto.convertFromExisting(
            readKeyFromFile(Configs.pubkeyPath),
            readKeyFromFile(Configs.privkeyPath)
        )
    }

    /**
     * Read key from file specified by [path]
     */
    private fun readKeyFromFile(path: String): String {
        try {
            return String(java.nio.file.Files.readAllBytes(Paths.get(path)))
        } catch (e: java.io.IOException) {
            logger.error("Unable to read key files.\n $e")
            System.exit(1)
        }
        return ""
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

    /** Add [commands] to transaction builder */
    private fun addCommandToBuilder(
        builder: ModelTransactionBuilder,
        commands: Array<IrohaCommand>
    ): ModelTransactionBuilder {
        var txBuilder = builder
        for (cmd in commands) {
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
        return txBuilder
    }

    /**
     * Provides dummy output to log just for verification
     */
    override fun onIrohaEvent(batch: IrohaOrderedBatch) {
        logger.info { "TX to IROHA" }

        // TODO rework with batch transactions
        for (tx in batch.transactions) {
            var txBuilder = ModelTransactionBuilder()
                .creatorAccountId(Configs.irohaCreator)
                .createdTime(BigInteger.valueOf(System.currentTimeMillis()))

            txBuilder = addCommandToBuilder(txBuilder, tx)
            val utx = txBuilder.build()

            // sign transaction and get its binary representation (Blob)
            val txblob = ModelProtoTransaction().signAndAddSignature(utx, keypair).blob()

            // Convert ByteVector to byte array
            val bs = toByteArray(txblob)

            // create proto object
            var protoTx: BlockOuterClass.Transaction? = null
            try {
                protoTx = BlockOuterClass.Transaction.parseFrom(bs)
            } catch (e: InvalidProtocolBufferException) {
                logger.error { "Exception while converting byte array to protobuf: ${e.message}" }
                System.exit(1)
            }

            // Send transaction to iroha
            val channel =
                ManagedChannelBuilder.forAddress(Configs.irohaHostname, Configs.irohaPort).usePlaintext(true).build()
            val stub = CommandServiceGrpc.newBlockingStub(channel)
            stub.torii(protoTx)

        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
