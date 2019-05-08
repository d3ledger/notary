/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.bootstrap.iroha

import com.github.kittinunf.result.Result
import iroha.protocol.Endpoint
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.TransactionStatusObserver
import jp.co.soramitsu.iroha.java.Utils
import jp.co.soramitsu.iroha.java.detail.InlineTransactionStatusObserver
import jp.co.soramitsu.iroha.java.subscription.WaitForTerminalStatus
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeoutException

/**
 * Statuses that we consider terminal
 */
private val terminalStatusesMST =
    Arrays.asList(
        Endpoint.TxStatus.MST_PENDING,
        Endpoint.TxStatus.STATELESS_VALIDATION_FAILED,
        Endpoint.TxStatus.STATEFUL_VALIDATION_FAILED,
        Endpoint.TxStatus.COMMITTED,
        Endpoint.TxStatus.MST_EXPIRED,
        //We don't consider this status terminal on purpose
        //Endpoint.TxStatus.NOT_RECEIVED,
        Endpoint.TxStatus.REJECTED,
        Endpoint.TxStatus.UNRECOGNIZED
    )

private val waitForTerminalStatusMST = WaitForTerminalStatus(terminalStatusesMST)

/**
 * Create tx status observer
 * @param txStatus - object that will hold tx status after observer completion
 * @return tx status observer
 */
private fun createTxStatusObserverMST(txStatus: TxStatus): InlineTransactionStatusObserver {
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
            txStatus.txException =
                Exception("Tx ${failedTx.txHash} failed. ${failedTx.errOrCmdName}")
        }
        .onUnrecognizedStatus { failedTx ->
            txStatus.txException =
                Exception("Tx ${failedTx.txHash} got unrecognized status. ${failedTx.errOrCmdName}")
        }
        .onTransactionCommitted { successTx -> txStatus.txHash = successTx.txHash.toUpperCase() }
        .onMstPending { pendingTx -> txStatus.txHash = pendingTx.txHash.toUpperCase() }
        .build()
}

/**
 * Send signed batch transaction to Iroha
 * @param transactions - transactions to send
 */
fun IrohaAPI.sendBatchMST(transactions: List<TransactionOuterClass.Transaction>): Result<Unit, Exception> {
    return Result.of {
        this.transactionListSync(transactions)
        transactions.map { tx -> Utils.hash(tx) }.forEach { txHash ->
            val txStatus = TxStatus.createEmpty()
            waitForTerminalStatusMST.subscribe(this, txHash)
                .blockingSubscribe(createTxStatusObserverMST(txStatus))
            txStatus.txException?.let { ex ->
                throw Exception("Iroha batch error", ex)
            }
        }
    }
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
