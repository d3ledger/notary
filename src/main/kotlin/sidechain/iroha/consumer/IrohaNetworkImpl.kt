package sidechain.iroha.consumer

import com.github.kittinunf.result.Result
import com.google.protobuf.ByteString
import iroha.protocol.Endpoint
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.Hash
import mu.KLogging
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.toByteArray

/**
 * Implements netwrok layer of Iroha chain
 */
class IrohaNetworkImpl(host: String, port: Int) : IrohaNetwork {

    /** Grpc stub for streaming output calls on the service */
    val toriiStub by lazy {
        ModelUtil.getCommandStub(host, port)
    }


    /**
     * Send transaction to iroha
     * @param protoTx protobuf representation of transaction
     */
    override fun send(protoTx: TransactionOuterClass.Transaction) {
        logger.info { "send TX to IROHA" }

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
    fun sendAndCheck(protoTx: TransactionOuterClass.Transaction, hash: Hash): Result<Unit, Exception> {
        send(protoTx)
        return checkTransactionStatus(hash)
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
