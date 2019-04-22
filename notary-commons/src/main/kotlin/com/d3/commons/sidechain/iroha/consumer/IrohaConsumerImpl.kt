package com.d3.commons.sidechain.iroha.consumer

import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.util.getAccountQuorum
import com.d3.commons.util.hex
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import iroha.protocol.Endpoint
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.*
import jp.co.soramitsu.iroha.java.detail.BuildableAndSignable
import jp.co.soramitsu.iroha.java.detail.InlineTransactionStatusObserver
import jp.co.soramitsu.iroha.java.subscription.WaitForTerminalStatus
import mu.KLogging
import java.io.IOException
import java.security.KeyPair
import java.util.*
import java.util.concurrent.TimeoutException

/**
 * Statuses that we consider terminal
 */
val terminalStatuses = listOf(
    Endpoint.TxStatus.STATELESS_VALIDATION_FAILED,
    Endpoint.TxStatus.STATEFUL_VALIDATION_FAILED,
    Endpoint.TxStatus.COMMITTED,
    Endpoint.TxStatus.MST_EXPIRED,
    //We don't consider this status terminal on purpose
    //Endpoint.TxStatus.NOT_RECEIVED,
    Endpoint.TxStatus.REJECTED,
    Endpoint.TxStatus.UNRECOGNIZED
)

/**
 * Endpoint of Iroha to write transactions
 * @param irohaCredential for creating transactions
 * @param irohaAPI Iroha network
 */
open class IrohaConsumerImpl(
    irohaCredential: IrohaCredential,
    private val irohaAPI: IrohaAPI
) : IrohaConsumer {
    override val keyPair = irohaCredential.keyPair

    private val queryAPI = QueryAPI(irohaAPI, irohaCredential.accountId, irohaCredential.keyPair)

    override val creator = irohaCredential.accountId

    protected open val waitForTerminalStatus = WaitForTerminalStatus(terminalStatuses)

    override fun getConsumerQuorum() = getAccountQuorum(queryAPI, creator)

    /**
     * Send transaction to Iroha and check if it is committed
     * @param utx - unsigned transaction to send
     * @return hex representation of hash or failure
     */
    override fun send(utx: Transaction): Result<String, Exception> {
        return sign(utx).flatMap { transaction ->
            send(transaction.build())
        }
    }

    /**
     * Send transaction to Iroha and check if it is committed
     * @param tx - built protobuf iroha transaction
     * @return hex representation of hash or failure
     */
    override fun send(tx: TransactionOuterClass.Transaction): Result<String, Exception> {
        return Result.of {
            val txStatus = TxStatus.createEmpty()
            irohaAPI.transaction(tx, waitForTerminalStatus)
                .blockingSubscribe(createTxStatusObserver(txStatus).build())
            if (txStatus.isSuccessful()) {
                txStatus.txHash!!
            } else {
                throw txStatus.txException!!
            }
        }
    }

    /**
     * Send list of transactions to Iroha and check if it is committed
     * @param lst - list of unsigned transactions to send
     * @return map of relevant hashes and boolean indicating if the tx is accepted by iroha
     */
    override fun send(lst: List<Transaction>): Result<Map<String, Boolean>, Exception> {
        val batch = lst.map { tx -> tx.build() }
        val irohaBatch = Utils.createTxOrderedBatch(batch, keyPair)
        val acceptedHashes = send(irohaBatch).get()
        return Result.of {
            irohaBatch.map { tx ->
                String.hex(Utils.hash(tx))
            }.map { hash ->
                hash to acceptedHashes.contains(hash)
            }.toMap()
        }
    }

    /**
     * Send list of transactions to Iroha as BATCH and check if it is committed
     * @param lst - list of built protobuf iroha transactions
     * @return list of hex hashes that were accepted by iroha
     */
    override fun send(lst: Iterable<TransactionOuterClass.Transaction>): Result<List<String>, Exception> {
        return Result.of {
            val successfulTxHashesList = ArrayList<String>()
            irohaAPI.transactionListSync(lst)
            lst.map { tx -> Utils.hash(tx) }.forEach { txHash ->
                val txStatus = TxStatus.createEmpty()
                waitForTerminalStatus.subscribe(irohaAPI, txHash)
                    .blockingSubscribe(createTxStatusObserver(txStatus).build())
                if (txStatus.isSuccessful()) {
                    successfulTxHashesList.add(txStatus.txHash!!)
                } else {
                    logger.error("Iroha batch error", txStatus.txException!!)
                }
            }
            successfulTxHashesList
        }
    }

    /**
     * Create tx status observer
     * @param txStatus - object that will hold tx status after observer completion
     * @return tx status observer
     */
    protected open fun createTxStatusObserver(txStatus: TxStatus):
            InlineTransactionStatusObserver.InlineTransactionStatusObserverBuilder {
        return TransactionStatusObserver.builder()
            .onError { ex -> txStatus.txException = IllegalStateException(ex) }
            .onMstExpired { expiredTx ->
                txStatus.fail(
                    TimeoutException("Tx ${expiredTx.txHash} MST expired. ${expiredTx.errOrCmdName}")
                )
            }
            .onNotReceived { failedTx ->
                txStatus.fail(
                    IOException("Tx ${failedTx.txHash} was not received. ${failedTx.errOrCmdName}")
                )
            }
            .onRejected { rejectedTx ->
                txStatus.fail(
                    IOException("Tx ${rejectedTx.txHash} was rejected. ${rejectedTx.errOrCmdName}")
                )
            }
            .onTransactionFailed { failedTx ->
                txStatus.fail(Exception("Tx ${failedTx.txHash} failed. ${failedTx.errOrCmdName}"))
            }
            .onUnrecognizedStatus { failedTx ->
                txStatus.fail(
                    Exception("Tx ${failedTx.txHash} got unrecognized status. ${failedTx.errOrCmdName}")
                )
            }
            .onTransactionCommitted { successTx -> txStatus.success(successTx.txHash.toUpperCase()) }
    }

    /**
     * Sign given IPJ transaction
     * @param utx - unsigned transaction
     */
    override fun sign(utx: Transaction): Result<BuildableAndSignable<TransactionOuterClass.Transaction>, Exception> {
        return Result.of { utx.sign(keyPair) }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}

/**
 * Data class that holds information about tx status
 * @param txHash - hash of transaction
 * @param txException - exception that occurs during transaction commitment
 */
data class TxStatus(var txHash: String?, var txException: Exception?) {

    /**
     * Marks transaction as successful
     * @param txHash - hash of successful transaction
     */
    fun success(txHash: String) {
        this.txHash = txHash
    }

    /**
     * Marks transaction as failed
     * @param ex - exception
     */
    fun fail(ex: Exception) {
        this.txException = ex
    }

    /**
     * Cheks if transaction is considered successful
     */
    fun isSuccessful() = txHash != null

    companion object {
        /**
         * Creates empty transaction status
         * @return empty status
         */
        fun createEmpty() = TxStatus(null, null)
    }
}
