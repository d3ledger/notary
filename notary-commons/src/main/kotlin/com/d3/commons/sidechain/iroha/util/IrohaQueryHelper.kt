/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.iroha.util

import com.github.kittinunf.result.Result
import iroha.protocol.QryResponses
import iroha.protocol.TransactionOuterClass
import java.util.*

interface IrohaQueryHelper {

    /**
     * Returns account id of a query creator
     */
    fun getQueryCreatorAccountId(): String

    /**
     * Retrieves account from Iroha
     * @param accountId - account to retrieve
     * @throws Exception if response contains error
     * @return account
     */
    fun getAccount(accountId: String): Result<QryResponses.AccountResponse, Exception>

    /**
     * Get signatories of an account
     * @param accountId - account to retrieve
     * @throws Exception if response contains error
     * @return account
     */
    fun getSignatories(accountId: String): Result<List<String>, Exception>

    /**
     * Retrieves account details by setter from Iroha
     * @param storageAccountId - account to read details from
     * @param writerAccountId - account that has set the details
     * @return Map with account details
     */
    fun getAccountDetails(
        storageAccountId: String,
        writerAccountId: String
    ): Result<Map<String, String>, Exception>

    /**
     * Retrieves account details by storage account id and key from Iroha
     * @param storageAccountId - account to read details from
     * @param key - key of detail
     * @return map with account details in form (setter->detail value)
     */
    fun getAccountDetailsByKeyOnly(
        storageAccountId: String,
        key: String
    ): Result<Map<String, String>, Exception>

    /**
     * Retrieves the first account detail by setter and predicate from Iroha
     * @param storageAccountId - account to read details from
     * @param writerAccountId - account that has set the details
     * @param firstPredicate - predicate
     * @return the first account detail by setter and [firstPredicate] or null
     */
    fun getAccountDetailsFirst(
        storageAccountId: String,
        writerAccountId: String,
        firstPredicate: (key: String, value: String) -> Boolean
    ): Result<Optional<Map.Entry<String, String>>, Exception>

    /**
     * Does the same exact thing as [getAccountDetailsFirst] but randomly shuffles every page before applying any predicate
     * @param storageAccountId - account to read details from
     * @param writerAccountId - account that has set the details
     * @param firstPredicate - predicate
     * @return the first account detail by setter and [firstPredicate] or null
     */
    fun getAccountDetailsFirstShufflePage(
        storageAccountId: String,
        writerAccountId: String,
        firstPredicate: (key: String, value: String) -> Boolean
    ): Result<Optional<Map.Entry<String, String>>, Exception>

    /**
     * Retrieves account details by setter filtered by predicate from Iroha
     * @param storageAccountId - account to read details from
     * @param writerAccountId - account that has set the details
     * @param filterPredicate - predicate
     * @return account details by setter filtered by [filterPredicate] from Iroha
     */
    fun getAccountDetailsFilter(
        storageAccountId: String,
        writerAccountId: String,
        filterPredicate: (key: String, value: String) -> Boolean
    ): Result<Map<String, String>, Exception>

    /**
     * Retrieves the number of account details by setter filtered by predicate from Iroha
     * @param storageAccountId - account to read details from
     * @param writerAccountId - account that has set the details
     * @param countPredicate - predicate
     * @return the number of account details by setter filtered by [countPredicate] from Iroha
     */
    fun getAccountDetailsCount(
        storageAccountId: String,
        writerAccountId: String,
        countPredicate: (key: String, value: String) -> Boolean
    ): Result<Int, Exception>

    /**
     * Retrieves account detail by setter and key from Iroha
     * @param storageAccountId - account to read details from
     * @param writerAccountId - account that has set the details
     * @param key - key of detail
     * @return optional detail
     */
    fun getAccountDetails(
        storageAccountId: String,
        writerAccountId: String,
        key: String
    ): Result<Optional<String>, Exception>

    /**
     * Get asset precision
     * @param assetId asset id in Iroha
     */
    fun getAssetPrecision(assetId: String): Result<Int, Exception>

    /**
     * Query Iroha account balance from [accountId].
     * @return Map(assetId to balance)
     */
    fun getAccountAssets(accountId: String): Result<Map<String, String>, Exception>

    /**
     * Get all account assets
     * @param accountId - account in Iroha
     * @param assetId - asset id in Iroha
     * @return asset account balance if asset is found, otherwise "0"
     */
    fun getAccountAsset(
        accountId: String,
        assetId: String
    ): Result<String, Exception>

    /**
     * Returns Iroha block by its height
     * @param height - height of Iroha block to get
     * @return Iroha block response
     */
    fun getBlock(height: Long): Result<QryResponses.BlockResponse, Exception>

    /**
     * Retrieves account quorum from Iroha
     * @param acc - account to read quorum from
     * @return account quorum
     */
    fun getAccountQuorum(acc: String): Result<Int, Exception>

    /**
     * Get transactions from Iroha by [hashes]
     * @param hashes - hex hashes of transactions
     * @return transaction
     */
    fun getTransactions(hashes: Iterable<String>): Result<QryResponses.TransactionsResponse, Exception>

    /**
     * Return first transaction from transaction query response
     * @param hash - hex hash of transaction
     * @return first transaction
     */
    fun getSingleTransaction(hash: String): Result<TransactionOuterClass.Transaction, Exception>

    /**
     * Return number of peers
     * @return number of peers
     */
    fun getPeersCount(): Result<Int, Exception>

    /**
     * Check if account is registered and has @publicKey
     */
    fun isRegistered(
        accountName: String,
        domainId: String,
        publicKey: String
    ): Result<Boolean, Exception>
}
