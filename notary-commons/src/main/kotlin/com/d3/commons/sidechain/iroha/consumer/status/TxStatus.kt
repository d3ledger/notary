/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.iroha.consumer.status

import jp.co.soramitsu.iroha.java.TransactionStatusObserver
import jp.co.soramitsu.iroha.java.detail.InlineTransactionStatusObserver
import java.io.IOException
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference


/**
 * Data class that holds information about tx status
 * @param state - state of a transaction
 * @param txException - exception that occurs during transaction commitment
 */
data class TxStatus(val state: TxState, val txException: Exception?) {

    /**
     * Checks if a transaction was successfully committed
     */
    fun isSuccessful() = state == TxState.COMMITTED

    companion object {
        /**
         * Creates successful transaction status
         * @return successful status
         */
        fun createSuccessful() = TxStatus(TxState.COMMITTED, null)

        /**
         * Creates failed transaction status
         * @param state- state of transaction
         * @param ex - exception
         * @return failed status
         */
        fun createFailed(state: TxState, ex: Exception) = TxStatus(state, ex)
    }
}

/**
 * Possible states of transactions
 */
enum class TxState {
    ERROR,
    MST_EXPIRED,
    NOT_RECEIVED,
    REJECTED,
    FAILED,
    UNRECOGNIZED_STATUS,
    COMMITTED
}

/**
 * Create tx status observer
 * @param statusReference - reference to an object that will hold tx status after observer completion
 * @return tx status observer
 */
fun createTxStatusObserver(statusReference: AtomicReference<TxStatus>):
        InlineTransactionStatusObserver.InlineTransactionStatusObserverBuilder {
    return TransactionStatusObserver.builder()
        .onError { ex ->
            statusReference.set(TxStatus.createFailed(TxState.ERROR, IllegalStateException(ex)))
        }
        .onMstExpired { expiredTx ->
            statusReference.set(
                TxStatus.createFailed(
                    TxState.MST_EXPIRED,
                    TimeoutException("Tx ${expiredTx.txHash} MST expired. ${expiredTx.errOrCmdName}")
                )
            )
        }
        .onNotReceived { failedTx ->
            statusReference.set(
                TxStatus.createFailed(
                    TxState.NOT_RECEIVED,
                    IOException("Tx ${failedTx.txHash} was not received. ${failedTx.errOrCmdName}")
                )
            )
        }
        .onRejected { rejectedTx ->
            statusReference.set(
                TxStatus.createFailed(
                    TxState.REJECTED,
                    IOException("Tx ${rejectedTx.txHash} was rejected. ${rejectedTx.errOrCmdName}")
                )
            )
        }
        .onTransactionFailed { failedTx ->
            statusReference.set(
                TxStatus.createFailed(
                    TxState.FAILED,
                    Exception("Tx ${failedTx.txHash} failed. ${failedTx.errOrCmdName}")
                )
            )
        }
        .onUnrecognizedStatus { failedTx ->
            statusReference.set(
                TxStatus.createFailed(
                    TxState.UNRECOGNIZED_STATUS,
                    Exception("Tx ${failedTx.txHash} got unrecognized status. ${failedTx.errOrCmdName}")
                )
            )
        }
        .onTransactionCommitted { successTx ->
            statusReference.set(TxStatus.createSuccessful())
        }
}
