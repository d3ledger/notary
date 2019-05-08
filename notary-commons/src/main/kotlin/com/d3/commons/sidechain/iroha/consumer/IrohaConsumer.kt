/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.iroha.consumer

import com.github.kittinunf.result.Result
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.detail.BuildableAndSignable

/**
 * Interface for consuming Iroha events provided by [notary.Notary]
 */
interface IrohaConsumer {

    /** Iroha transactions creator */
    val creator: String

    /**
     * Returns consumer quorum value
     */
    fun getConsumerQuorum(): Result<Int, Exception>

    /**
     * Send transaction to Iroha and check if it is committed
     * @param utx - unsigned transaction to sign and send
     */
    fun send(utx: Transaction): Result<String, Exception>

    /**
     * Send transaction to Iroha and check if it is committed
     * @param tx - built protobuf iroha transaction
     */
    fun send(tx: TransactionOuterClass.Transaction): Result<String, Exception>

    /**
     * Send list of transactions to Iroha and check if it is committed
     * @param lst - list of unsigned transactions to sign and send
     */
    fun send(lst: List<Transaction>): Result<Map<String, Boolean>, Exception>

    /**
     * Send list of transactions to Iroha as BATCH and check if it is committed
     * @param lst - list of built protobuf iroha transactions
     */
    fun send(lst: Iterable<TransactionOuterClass.Transaction>): Result<List<String>, Exception>

    /**
     * Sign given IPJ transaction
     * @param utx - unsigned transaction
     */
    fun sign(utx: Transaction): Result<BuildableAndSignable<TransactionOuterClass.Transaction>, Exception>
}
