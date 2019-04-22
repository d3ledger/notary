/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.iroha.util.impl

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.d3.commons.sidechain.iroha.util.getErrorMessage
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import iroha.protocol.QryResponses
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Query
import jp.co.soramitsu.iroha.java.QueryAPI
import java.security.KeyPair
import java.util.concurrent.atomic.AtomicLong

/**
 * The purpose of the class is to hide Iroha query implementation.
 */
class IrohaQueryHelperImpl(val queryAPI: QueryAPI) : IrohaQueryHelper {

    constructor(irohaAPI: IrohaAPI, accountId: String, keyPair: KeyPair) : this(
        QueryAPI(
            irohaAPI,
            accountId,
            keyPair
        )
    )

    //Query counter
    private val queryCounter = AtomicLong(1)

    /** {@inheritDoc} */
    override fun getAccount(accountId: String): QryResponses.AccountResponse {
        val q = Query.builder(
            queryAPI.accountId,
            queryCounter.getAndIncrement()
        )
            .getAccount(accountId)
            .buildSigned(queryAPI.keyPair)
        val res = queryAPI.api.query(q)
        if (res.hasErrorResponse()) {
            val errorResponse = res.errorResponse
            throw Exception("Cannot get account. ${getErrorMessage(errorResponse)}")
        }
        return res.accountResponse
    }

    /** {@inheritDoc} */
    override fun getAccountData(acc: String): Result<JsonObject, Exception> {
        return Result.of { getAccount(acc) }.map { queryResponse ->
            val stringBuilder = StringBuilder(queryResponse.account.jsonData)
            Parser().parse(stringBuilder) as JsonObject
        }
    }

    /** {@inheritDoc} */
    override fun getAccountDetails(
        storageAccountId: String,
        writerAccountId: String
    ): Result<Map<String, String>, Exception> {
        return getAccountData(storageAccountId).map { json ->
            if (json.map[writerAccountId] == null)
                mutableMapOf()
            else
                json.map[writerAccountId] as MutableMap<String, String>
        }
    }

    /** {@inheritDoc} */
    override fun getAssetPrecision(assetId: String): Result<Int, Exception> {
        return Result.of { queryAPI.getAssetInfo(assetId) }
            .map { queryResponse ->
                val asset = queryResponse.asset
                if (asset.assetId.isNullOrEmpty()) {
                    throw Exception("There is no such asset $assetId.")
                }
                asset.precision
            }
    }

    /** {@inheritDoc} */
    override fun getAccountAssets(accountId: String): Result<Map<String, String>, Exception> {
        return Result.of {
            queryAPI.getAccountAssets(accountId).accountAssetsList.associate { asset ->
                asset.assetId to asset.balance
            }
        }
    }

    /** {@inheritDoc} */
    override fun getAccountAsset(
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

    /** {@inheritDoc} */
    override fun getBlockRawResponse(height: Long): QryResponses.QueryResponse {
        val q = Query.builder(queryAPI.accountId, queryCounter.getAndIncrement())
            .getBlock(height)
            .buildSigned(queryAPI.keyPair)
        return queryAPI.api.query(q)
    }

    /** {@inheritDoc} */
    override fun getBlock(height: Long): QryResponses.BlockResponse {
        val res = getBlockRawResponse(height)
        if (res.hasErrorResponse()) {
            val errorResponse = res.errorResponse
            throw Exception("Cannot get block. ${getErrorMessage(errorResponse)}")
        }
        return res.blockResponse
    }

    /** {@inheritDoc} */
    override fun getAccountQuorum(acc: String): Result<Int, Exception> {
        return Result.of { getAccount(acc) }
            .map { queryResponse ->
                queryResponse.account.quorum
            }
    }

    /** {@inheritDoc} */
    override fun getTransactions(hashes: Iterable<String>): Result<QryResponses.TransactionsResponse, Exception> {
        return Result.of { queryAPI.getTransactions(hashes) }
    }

    /** {@inheritDoc} */
    override fun getSingleTransaction(hash: String): Result<TransactionOuterClass.Transaction, Exception> {
        return getTransactions(listOf(hash)).map { queryResponse ->
            if (queryResponse.transactionsCount == 0)
                throw Exception("There is no transactions.")

            queryResponse.transactionsList[0]
        }
    }
}
