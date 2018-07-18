package sidechain.iroha.consumer

import com.github.kittinunf.result.Result
import com.google.protobuf.ByteString
import iroha.protocol.BlockOuterClass
import iroha.protocol.Endpoint
import iroha.protocol.Responses
import jp.co.soramitsu.iroha.Hash
import mu.KLogging
import notary.endpoint.eth.NotaryException
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.toByteArray

/**
 * Implements netwrok layer of Iroha chain
 */
class IrohaNetworkImpl(host: String, port: Int) : IrohaNetwork {

    val channel = ModelUtil.getChannel(host, port)

    /** Grpc stub for streaming output calls on the service */
    val commnandStub by lazy {
        ModelUtil.getCommandStub(channel)
    }

    val queryStub by lazy {
        ModelUtil.getQueryStub(channel)
    }

    /**
     * Send transaction to iroha
     * @param protoTx protobuf representation of transaction
     */
    override fun send(protoTx: BlockOuterClass.Transaction) {
        logger.info { "send TX to IROHA" }

        // Send transaction to iroha
        commnandStub.torii(protoTx)
    }

    /**
     * Check if transaction is committed to Iroha
     */
    fun checkTransactionStatus(hash: Hash): Result<Unit, Exception> {
        return Result.of {
            val bhash = hash.blob().toByteArray()

            val request = Endpoint.TxStatusRequest.newBuilder().setTxHash(ByteString.copyFrom(bhash)).build()
            val response = commnandStub.statusStream(request)

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
    override fun sendAndCheck(protoTx: BlockOuterClass.Transaction, hash: Hash): Result<Unit, Exception> {
        send(protoTx)
        return checkTransactionStatus(hash)
    }

    /** Send query and check result */
    override fun sendQuery(protoQuery: iroha.protocol.Queries.Query): Result<Responses.QueryResponse, Exception> {
        return Result.of { queryStub.find(protoQuery) }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
