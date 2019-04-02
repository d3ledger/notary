/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.iroha.util

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import iroha.protocol.BlockOuterClass
import iroha.protocol.Commands
import iroha.protocol.QryResponses
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.Query
import jp.co.soramitsu.iroha.java.QueryAPI
import java.util.concurrent.atomic.AtomicLong

//Query counter
private val queryCounter = AtomicLong(1)

/**
 * Get asset precision
 *
 * @param queryAPI - iroha queries network layer
 * @param assetId asset id in Iroha
 */
fun getAssetPrecision(
    queryAPI: QueryAPI,
    assetId: String
): Result<Int, Exception> {
    return Result.of { queryAPI.getAssetInfo(assetId) }
        .map { queryResponse ->
            val asset = queryResponse.asset
            if (asset.assetId.isNullOrEmpty()) {
                throw Exception("There is no such asset $assetId.")
            }
            asset.precision
        }
}

/**
 * Get all account assets
 *
 * @param queryAPI - iroha queries network layer
 * @param accountId - account in Iroha
 * @param assetId - asset id in Iroha
 *
 * @return asset account balance if asset is found, otherwise "0"
 */
fun getAccountAsset(
    queryAPI: QueryAPI,
    accountId: String,
    assetId: String
): Result<String, Exception> {
    return Result.of { queryAPI.getAccountAssets(accountId) }
        .map { queryResponse ->
            val accountAsset = queryResponse.accountAssetsList
                .find { it.assetId == assetId }

            accountAsset?.balance ?: "0"
        }
}

/**
 * Retrieves account JSON data from Iroha
 * @param queryAPI - iroha queries network layer
 * @param acc - account to retrieve details from
 * @return Map with account details
 */
fun getAccountData(
    queryAPI: QueryAPI,
    acc: String
): Result<JsonObject, Exception> {
    return Result.of { getAccount(queryAPI, acc) }
        .map { queryResponse ->
            val stringBuilder = StringBuilder(queryResponse.account.jsonData)
            Parser().parse(stringBuilder) as JsonObject
        }
}

/**
 * Retrieves account from Iroha
 * @param queryAPI - Iroha queries network layer
 * @param accountId - account to retrieve
 * @throws Exception if response contains error
 * @return account
 */
private fun getAccount(queryAPI: QueryAPI, accountId: String): QryResponses.AccountResponse {
    val q = Query.builder(queryAPI.accountId, queryCounter.getAndIncrement())
        .getAccount(accountId)
        .buildSigned(queryAPI.keyPair)
    val res = queryAPI.api.query(q)
    if (res.hasErrorResponse()) {
        val errorResponse = res.errorResponse
        throw Exception("Cannot get account. ${getErrorMessage(errorResponse)}")
    }
    return res.accountResponse
}

/**
 * Returns raw query response with Iroha block.
 * May be used to handle Iroha error codes manually
 * @param queryAPI - Iroha queries network layer
 * @param height - height of Iroha block to get
 * @return Iroha block
 */
fun getBlockRawResponse(queryAPI: QueryAPI, height: Long): QryResponses.QueryResponse {
    val q = Query.builder(queryAPI.accountId, queryCounter.getAndIncrement())
        .getBlock(height)
        .buildSigned(queryAPI.keyPair)
    return queryAPI.api.query(q);
}

/**
 * Returns Iroha block by its height
 * @param queryAPI - Iroha queries network layer
 * @param height - height of Iroha block to get
 * @return Iroha block response
 */
fun getBlock(queryAPI: QueryAPI, height: Long): QryResponses.BlockResponse {
    val res = getBlockRawResponse(queryAPI, height)
    if (res.hasErrorResponse()) {
        val errorResponse = res.errorResponse
        throw Exception("Cannot get block. ${getErrorMessage(errorResponse)}")
    }
    return res.blockResponse
}

/**
 * Returns pretty formatted error message. May be used in exceptions.
 * @param errorResponse - error response that is used to created pretty error message
 */
fun getErrorMessage(errorResponse: QryResponses.ErrorResponse) =
    "Error code ${errorResponse.errorCode} reason ${errorResponse.reason} ${errorResponse.message}"


/**
 * Retrieves account quorum from Iroha
 * @param queryAPI - iroha queries network layer
 * @param acc - account to read quorum from
 * @return account quorum
 */
fun getAccountQuorum(
    queryAPI: QueryAPI,
    acc: String
): Result<Int, Exception> {
    return Result.of { getAccount(queryAPI, acc) }
        .map { queryResponse ->
            queryResponse.account.quorum
        }
}

/**
 * Retrieves account details by setter from Iroha
 * @param queryAPI - iroha queries network layer
 * @param acc - account to read details from
 * @param detailSetterAccount - account that has set the details
 * @return Map with account details
 */
fun getAccountDetails(
    queryAPI: QueryAPI,
    acc: String,
    detailSetterAccount: String
): Result<MutableMap<String, String>, Exception> {
    return getAccountData(
        queryAPI,
        acc
    ).map { json ->
        if (json.map[detailSetterAccount] == null)
            mutableMapOf()
        else
            json.map[detailSetterAccount] as MutableMap<String, String>
    }
}

/**
 * Get transactions from Iroha by [hashes]
 * @param queryAPI - iroha queries network layer
 * @param hashes - hex hashes of transactions
 * @return transaction
 */
fun getTransactions(
    queryAPI: QueryAPI,
    hashes: Iterable<String>
): Result<QryResponses.TransactionsResponse, Exception> {
    return Result.of { queryAPI.getTransactions(hashes) }
}

/**
 * Return first transaction from transaction query response
 * @param queryAPI - iroha queries network layer
 * @param hash - hex hash of transaction
 * @return first transaction
 */
fun getSingleTransaction(
    queryAPI: QueryAPI,
    hash: String
): Result<TransactionOuterClass.Transaction, Exception> {
    return getTransactions(queryAPI, listOf(hash)).map { queryResponse ->
        if (queryResponse.transactionsCount == 0)
            throw Exception("There is no transactions.")

        // return transaction
        queryResponse.transactionsList[0]
    }
}

/**
 * Return all "set account detail" commands from Iroha block
 * @param block - Iroha block
 * @return list full of "set account detail" commands
 */
fun getSetDetailCommands(block: BlockOuterClass.Block): List<Commands.Command> {
    return block.blockV1.payload.transactionsList.flatMap { tx ->
        tx.payload.reducedPayload.commandsList
    }.filter { command -> command.hasSetAccountDetail() }
}

/**
 * Return all "transfer asset" commands from Iroha block
 * @param block - Iroha block
 * @return list full of "transfer asset" commands
 */
fun getTransferCommands(block: BlockOuterClass.Block): List<Commands.Command> {
    return block.blockV1.payload.transactionsList.flatMap { tx -> tx.payload.reducedPayload.commandsList }
        .filter { command -> command.hasTransferAsset() }
}
