package sidechain.iroha.consumer

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.google.protobuf.ByteString
import iroha.protocol.Endpoint
import iroha.protocol.QryResponses
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.Hash
import mu.KLogging
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.toByteArray

/**
 * Implements netwrok layer of Iroha chain
 */
class IrohaNetworkImpl(host: String, port: Int) : IrohaNetwork {

    val channel = ModelUtil.getChannel(host, port)

    /** Grpc stub for streaming output calls on the service */
    val commandStub by lazy {
        ModelUtil.getCommandStub(channel)
    }

    val queryStub by lazy {
        ModelUtil.getQueryStub(channel)
    }

    /**
     * Send transaction to iroha
     * @param protoTx protobuf representation of transaction
     */
    fun send(protoTx: TransactionOuterClass.Transaction) {
        logger.info { "send TX to IROHA" }

        // Send transaction to iroha
        commandStub.torii(protoTx)
    }

    /**
     * Check if transaction is committed to Iroha
     * @param hash - hash of transaction to check
     * @return string representation of hash or failure
     */
    fun checkTransactionStatus(hash: Hash): Result<String, Exception> {
        return Result.of {
            val bhash = hash.blob().toByteArray()

            val request = Endpoint.TxStatusRequest.newBuilder().setTxHash(ByteString.copyFrom(bhash)).build()
            val response = commandStub.statusStream(request)

            while (response.hasNext()) {
                val res = response.next()
                if (res.txStatus.name == "STATEFUL_VALIDATION_FAILED") {
                    logger.error { "Iroha transaction ${hash.hex()} received STATEFUL_VALIDATION_FAILED" }
                    throw Exception("Iroha transaction ${hash.hex()} received STATEFUL_VALIDATION_FAILED")
                }
            }

            hash.hex()
        }
    }

    /**
     * Send transaction to Iroha and check it's status
     *
     * @param tx - transaction
     * @param hash - transaction hash
     * @return string representation of transaction hash or Exception has raised
     */
    override fun sendAndCheck(tx: TransactionOuterClass.Transaction, hash: Hash): Result<String, Exception> {
        return Result.of { send(tx) }
            .flatMap { checkTransactionStatus(hash) }
    }

    /**
     * Send query and check result
     *
     * @param protoQuery - iroha protocol query
     * @return Query response on success or Exception on failure
     */
    override fun sendQuery(protoQuery: iroha.protocol.Queries.Query): Result<QryResponses.QueryResponse, Exception> {
        return Result.of { queryStub.find(protoQuery) }
    }

    /**
     * Shutdown channel
     */
    fun shutdown() {
        channel.shutdown()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
