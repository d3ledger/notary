/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.iroha.consumer

import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.consumer.status.IrohaTxStatus
import com.d3.commons.sidechain.iroha.consumer.status.createTxStatusObserver
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import com.d3.commons.util.hex
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import iroha.protocol.Endpoint
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.Utils
import jp.co.soramitsu.iroha.java.detail.BuildableAndSignable
import jp.co.soramitsu.iroha.java.subscription.WaitForTerminalStatus
import mu.KLogging
import java.util.*
import java.util.concurrent.atomic.AtomicReference

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
    private val keyPair = irohaCredential.keyPair

    private val queryHelper =
        IrohaQueryHelperImpl(irohaAPI, irohaCredential.accountId, irohaCredential.keyPair)

    override val creator = irohaCredential.accountId

    protected open val waitForTerminalStatus = WaitForTerminalStatus(terminalStatuses)

    override fun getConsumerQuorum() = queryHelper.getAccountQuorum(creator)

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
            val statusReference = AtomicReference<IrohaTxStatus>()
            irohaAPI.transaction(tx, waitForTerminalStatus)
                .blockingSubscribe(getTxStatusObserver(statusReference).build())
            if (statusReference.get().isSuccessful()) {
                String.hex(Utils.hash(tx))
            } else {
                throw statusReference.get().txException!!
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
                val statusReference = AtomicReference<IrohaTxStatus>()
                waitForTerminalStatus.subscribe(irohaAPI, txHash)
                    .blockingSubscribe(getTxStatusObserver(statusReference).build())
                if (statusReference.get().isSuccessful()) {
                    successfulTxHashesList.add(String.hex(txHash))
                } else {
                    logger.error("Iroha batch error", statusReference.get().txException!!)
                }
            }
            successfulTxHashesList
        }
    }

    /**
     * Create tx status observer.
     * @param statusReference - reference to an object that will hold tx status after observer completion
     * @return tx status observer
     */
    protected open fun getTxStatusObserver(statusReference: AtomicReference<IrohaTxStatus>) =
        createTxStatusObserver(statusReference)

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
