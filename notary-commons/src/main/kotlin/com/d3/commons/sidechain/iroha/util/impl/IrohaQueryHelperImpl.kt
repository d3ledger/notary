/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.iroha.util.impl

import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import iroha.protocol.QryResponses
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import java.security.KeyPair

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

    constructor(irohaAPI: IrohaAPI, irohaCredential: IrohaCredential) : this(
        QueryAPI(
            irohaAPI,
            irohaCredential.accountId,
            irohaCredential.keyPair
        )
    )

    private val gson = Gson()

    /**
     * Deserialise JSON string to Map<String, String>
     *
     * @param jsonStr JSON as String
     * @return desereialized details as (writer -> (key -> value))
     */
    private fun parseAccountDetailsJson(jsonStr: String): Result<Map<String, Map<String, String>>, Exception> {
        return Result.of {
            val responseType = object : TypeToken<Map<String, Map<String, String>>>() {}.getType()
            gson.fromJson<Map<String, Map<String, String>>>(jsonStr, responseType)
        }
    }

    /** {@inheritDoc} */
    override fun getAccount(accountId: String): Result<QryResponses.AccountResponse, Exception> {
        return Result.of { queryAPI.getAccount(accountId) }
    }

    /** {@inheritDoc} */
    override fun getAccountDetails(
        storageAccountId: String,
        writerAccountId: String
    ): Result<Map<String, String>, Exception> {
        return Result.of { queryAPI.getAccountDetails(storageAccountId, writerAccountId, null) }
            .flatMap { str -> parseAccountDetailsJson(str) }
            .map { details ->
                if (details.get(writerAccountId) == null)
                    emptyMap()
                else
                    details.getOrDefault(writerAccountId, emptyMap())
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
    override fun getBlock(height: Long): Result<QryResponses.BlockResponse, Exception> {
        return Result.of { queryAPI.getBlock(height) }
    }

    /** {@inheritDoc} */
    override fun getAccountQuorum(acc: String): Result<Int, Exception> {
        return getAccount(acc).map { queryResponse ->
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
