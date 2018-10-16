package sidechain.iroha.consumer

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.google.protobuf.ByteString
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import iroha.protocol.Endpoint
import iroha.protocol.QryResponses
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.Hash
import jp.co.soramitsu.iroha.ModelBlocksQueryBuilder
import model.IrohaCredential
import mu.KLogging
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.toByteArray
import java.math.BigInteger
import java.util.concurrent.TimeUnit

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
     * Send transaction batch to iroha
     * @param batch list of protobuf representation of transactions
     */
    fun send(batch: List<TransactionOuterClass.Transaction>) {
        logger.info { "send TX batch to IROHA" }

        // Send transaction to iroha
        val lst = Endpoint.TxList.newBuilder()
        batch.forEach { lst.addTransactions(it) }
        commandStub.listTorii(lst.build())
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
                    val message =
                        "Iroha transaction ${hash.hex()} received STATEFUL_VALIDATION_FAILED ${res.errorMessage}"
                    logger.error { message }
                    throw Exception(message)
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
     * Send batch of transaction to Iroha and check it's status
     *
     * @param batch - batch of transactions
     * @param hashes - transactions hashes
     * @return list of hashes that were accepted by iroha
     */
    override fun sendAndCheck(
        batch: List<TransactionOuterClass.Transaction>,
        hashes: List<Hash>
    ): Result<List<String>, Exception> {
        send(batch)

        return Result.of {
            hashes.map { checkTransactionStatus(it) }
                .filter {
                    it.fold(
                        { true },
                        {
                            logger.warn("Batch tx was failed: ", it)
                            false
                        }
                    )
                }.map { it.get() }
        }
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
     * Get block streaming.
     */
    override fun getBlocksStreaming(credential: IrohaCredential): Result<Observable<QryResponses.BlockQueryResponse>, Exception> {
        return Result.of {
            val uquery = ModelBlocksQueryBuilder()
                .creatorAccountId(credential.accountId)
                .createdTime(ModelUtil.getCurrentTime())
                .queryCounter(BigInteger.valueOf(1))
                .build()
            val query = ModelUtil.prepareBlocksQuery(uquery, credential.keyPair)!!

            queryStub.fetchCommits(query).toObservable()
        }
    }

    /**
     * Shutdown channel
     */
    override fun close() {
        channel.shutdown()
        try {
            channel.awaitTermination(1, TimeUnit.SECONDS)
        } catch (ex: InterruptedException) {
            logger.warn { ex }
            channel.shutdownNow()
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
