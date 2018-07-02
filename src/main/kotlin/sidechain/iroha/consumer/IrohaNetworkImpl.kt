package sidechain.iroha.consumer

import com.github.kittinunf.result.Result
import com.google.protobuf.ByteString
import io.grpc.ManagedChannelBuilder
import iroha.protocol.BlockOuterClass
import iroha.protocol.CommandServiceGrpc
import iroha.protocol.Endpoint
import jp.co.soramitsu.iroha.Hash
import main.ConfigKeys
import mu.KLogging
import notary.CONFIG
import sidechain.iroha.util.toByteArray

/**
 * Implements netwrok layer of Iroha chain
 */
class IrohaNetworkImpl : IrohaNetwork {

    /** Grpc stub for streaming output calls on the service */
    val toriiStub: iroha.protocol.CommandServiceGrpc.CommandServiceBlockingStub

    init {
        val channel =
            ManagedChannelBuilder.forAddress(CONFIG[ConfigKeys.irohaHostname], CONFIG[ConfigKeys.irohaPort])
                .usePlaintext(true).build()
        toriiStub = CommandServiceGrpc.newBlockingStub(channel)
    }

    /**
     * Send transaction to iroha
     * @param protoTx protobuf representation of transaction
     */
    override fun send(protoTx: BlockOuterClass.Transaction) {
        logger.info { "TX to IROHA" }

        // Send transaction to iroha
        toriiStub.torii(protoTx)
    }

    /**
     * Check if transaction is committed to Iroha
     */
    fun checkTransactionStatus(hash: Hash): Result<Unit, Exception> {
        return Result.of {
            val bhash = hash.blob().toByteArray()

            val request = Endpoint.TxStatusRequest.newBuilder().setTxHash(ByteString.copyFrom(bhash)).build()
            val response = toriiStub.statusStream(request)

            while (response.hasNext()) {
                val res = response.next()
                if (res.txStatus.name == "STATEFUL_VALIDATION_FAILED") {
                    logger.error { "Iroha transacion ${hash.hex()} received STATEFUL_VALIDATION_FAILED" }
                    throw Exception("Iroha transacion ${hash.hex()} received STATEFUL_VALIDATION_FAILED")
                }
            }
        }
    }

    /**
     * Send and check transaction to Iroha
     */
    fun sendAndCheck(protoTx: BlockOuterClass.Transaction, hash: Hash): Result<Unit, Exception> {
        send(protoTx)
        return checkTransactionStatus(hash)
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
