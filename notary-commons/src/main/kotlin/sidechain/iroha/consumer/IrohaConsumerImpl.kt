package sidechain.iroha.consumer

import com.github.kittinunf.result.Result
import iroha.protocol.Endpoint
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.TransactionStatusObserver
import jp.co.soramitsu.iroha.java.Utils
import jp.co.soramitsu.iroha.java.detail.InlineTransactionStatusObserver
import jp.co.soramitsu.iroha.java.subscription.WaitForTerminalStatus
import model.IrohaCredential
import mu.KLogging
import util.hex
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeoutException

/**
 * Statuses that we consider terminal
 */
private val terminalStatuses =
    Arrays.asList(
        Endpoint.TxStatus.STATELESS_VALIDATION_FAILED,
        Endpoint.TxStatus.STATEFUL_VALIDATION_FAILED,
        Endpoint.TxStatus.COMMITTED,
        Endpoint.TxStatus.MST_EXPIRED,
        //We don't consider this status terminal on purpose
        //Endpoint.TxStatus.NOT_RECEIVED,
        Endpoint.TxStatus.REJECTED,
        Endpoint.TxStatus.UNRECOGNIZED
    )

private val waitForTerminalStatus = WaitForTerminalStatus(terminalStatuses)

/**
 * Endpoint of Iroha to write transactions
 * @param irohaCredential for creating transactions
 * @param irohaAPI Iroha network
 */
class IrohaConsumerImpl(
    irohaCredential: IrohaCredential,
    private val irohaAPI: IrohaAPI
) : IrohaConsumer {
    override val creator = irohaCredential.accountId

    val keypair = irohaCredential.keyPair

    /**
     * Send transaction to Iroha and check if it is committed
     * @param utx - unsigned transaction to send
     * @return hex representation of hash or failure
     */
    override fun send(utx: Transaction): Result<String, Exception> {
        val transaction = utx.sign(keypair).build()
        return send(transaction)
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
                .blockingSubscribe(createTxStatusObserver(txStatus))
            when {
                txStatus.txHash != null -> txStatus.txHash!!
                txStatus.txException != null -> throw txStatus.txException!!
                else -> throw IllegalStateException("Transaction has no errors or signs of completion")
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
        val irohaBatch = Utils.createTxOrderedBatch(batch, keypair)
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
                    .blockingSubscribe(createTxStatusObserver(txStatus))
                if (txStatus.txHash != null) {
                    successfulTxHashesList.add(txStatus.txHash!!)
                } else if (txStatus.txException != null) {
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
    private fun createTxStatusObserver(txStatus: TxStatus): InlineTransactionStatusObserver {
        return TransactionStatusObserver.builder()
            .onError { ex -> txStatus.txException = IllegalStateException(ex) }
            .onMstExpired { expiredTx ->
                txStatus.txException =
                        TimeoutException("Tx ${expiredTx.txHash} MST expired. ${expiredTx.errOrCmdName}")
            }
            .onNotReceived { failedTx ->
                txStatus.txException =
                        IOException("Tx ${failedTx.txHash} was not received. ${failedTx.errOrCmdName}")
            }
            .onRejected { rejectedTx ->
                txStatus.txException =
                        IOException("Tx ${rejectedTx.txHash} was rejected. ${rejectedTx.errOrCmdName}")
            }
            .onTransactionFailed { failedTx ->
                txStatus.txException = Exception("Tx ${failedTx.txHash} failed. ${failedTx.errOrCmdName}")
            }
            .onUnrecognizedStatus { failedTx ->
                txStatus.txException =
                        Exception("Tx ${failedTx.txHash} got unrecognized status. ${failedTx.errOrCmdName}")
            }
            .onTransactionCommitted { successTx -> txStatus.txHash = successTx.txHash.toUpperCase() }
            .build()
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
private data class TxStatus(var txHash: String?, var txException: Exception?) {
    companion object {
        fun createEmpty() = TxStatus(null, null)
    }
}
