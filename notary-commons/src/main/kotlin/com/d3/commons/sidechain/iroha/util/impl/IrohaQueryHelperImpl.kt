/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.iroha.util.impl

import com.d3.commons.config.IrohaCredentialRawConfig
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.d3.commons.util.GsonInstance
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import com.google.gson.reflect.TypeToken
import iroha.protocol.QryResponses
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import java.security.KeyPair
import java.util.*

/**
 * The purpose of the class is to hide Iroha query implementation.
 * @param queryAPI - query API by Iroha-Java library
 * @param pageSize - number of items per one Iroha query. 100 by default
 */
open class IrohaQueryHelperImpl(val queryAPI: QueryAPI, val pageSize: Int = 100) : IrohaQueryHelper {

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

    constructor(irohaAPI: IrohaAPI, irohaCredentialRawConfig: IrohaCredentialRawConfig) : this(
        irohaAPI,
        IrohaCredential(irohaCredentialRawConfig)
    )

    private val gson = GsonInstance.get()

    private fun getPaginatedAccountDetails(
        storageAccountId: String?,
        writerAccountId: String?,
        key: String?
    ): Result<Map<String, Map<String, String>>, Exception> {
        return Result.of {
            var lastWriter: String? = null
            var lastKey: String? = null

            val detailsMap = mutableMapOf<String, MutableMap<String, String>>()

            do {
                val response = queryAPI.getAccountDetails(
                    storageAccountId,
                    writerAccountId,
                    key,
                    pageSize,
                    lastWriter,
                    lastKey
                )
                lastWriter = response.nextRecordId.writer
                lastKey = response.nextRecordId.key

                val toAdd = parseAccountDetailsJson(response.detail).get()
                toAdd.entries.forEach { (writer, details) ->
                    detailsMap.merge(writer, details.toMutableMap()) { oldVal, newVal ->
                        oldVal.putAll(newVal)
                        oldVal
                    }
                }
            } while (response.hasNextRecordId())

            detailsMap
        }
    }

    private fun getPaginatedAccountAssets(
        accountId: String
    ): Result<Map<String, String>, Exception> {
        return Result.of {
            var lastAssetId: String? = null

            val assetMap = mutableMapOf<String, String>()

            do {
                val response = queryAPI.getAccountAssets(
                    accountId,
                    pageSize,
                    lastAssetId
                )
                lastAssetId = response.nextAssetId
                assetMap.putAll(response.accountAssetsList.associate { asset ->
                    asset.assetId to asset.balance
                })
            } while (response.nextAssetId != null && response.nextAssetId != "")

            assetMap
        }
    }

    /**
     * Deserialise JSON string to Map<String, String>
     *
     * @param jsonStr JSON as String
     * @return desereialized details as (writer -> (key -> value))
     */
    private fun parseAccountDetailsJson(jsonStr: String): Result<Map<String, Map<String, String>>, Exception> =
        Result.of {
            val responseType = object : TypeToken<Map<String, Map<String, String>>>() {}.type
            gson.fromJson<Map<String, Map<String, String>>>(jsonStr, responseType)
        }

    /** {@inheritDoc} */
    override fun getAccount(accountId: String): Result<QryResponses.AccountResponse, Exception> {
        return Result.of { queryAPI.getAccount(accountId) }
    }

    /** {@inheritDoc} */
    override fun getSignatories(accountId: String): Result<List<String>, Exception> =
        Result.of { queryAPI.getSignatories(accountId).keysList }

    /** {@inheritDoc} */
    override fun getAccountDetails(
        storageAccountId: String,
        writerAccountId: String
    ): Result<Map<String, String>, Exception> =
        getPaginatedAccountDetails(storageAccountId, writerAccountId, null)
            .map { details ->
                if (details[writerAccountId] == null)
                    emptyMap()
                else
                    details.getOrDefault(writerAccountId, emptyMap())
            }

    /** {@inheritDoc} */
    override fun getAccountDetails(
        storageAccountId: String,
        writerAccountId: String,
        key: String
    ): Result<Optional<String>, Exception> =
        getPaginatedAccountDetails(storageAccountId, writerAccountId, key)
            .map { details ->
                Optional.ofNullable(details.getOrDefault(writerAccountId, emptyMap())[key])
            }

    /** {@inheritDoc} */
    override fun keyExistsInDetails(storageAccountId: String, key: String): Result<Boolean, Exception> =
        getPaginatedAccountDetails(
            storageAccountId,
            writerAccountId = null,
            key = key
        ).map { details -> !details.isEmpty() }

    /** {@inheritDoc} */
    override fun getAssetPrecision(assetId: String): Result<Int, Exception> =
        Result.of { queryAPI.getAssetInfo(assetId) }
            .map { queryResponse ->
                val asset = queryResponse.asset
                if (asset.assetId.isNullOrEmpty()) {
                    throw Exception("There is no such asset $assetId.")
                }
                asset.precision
            }

    /** {@inheritDoc} */
    override fun getAccountAssets(accountId: String): Result<Map<String, String>, Exception> =
        getPaginatedAccountAssets(accountId)

    /** {@inheritDoc} */
    override fun getAccountAsset(accountId: String, assetId: String): Result<String, Exception> =
        getPaginatedAccountAssets(accountId)
            .map { it.getOrDefault(assetId, "0") }

    /** {@inheritDoc} */
    override fun getBlock(height: Long): Result<QryResponses.BlockResponse, Exception> =
        Result.of { queryAPI.getBlock(height) }


    /** {@inheritDoc} */
    override fun getAccountQuorum(acc: String): Result<Int, Exception> =
        getAccount(acc).map { queryResponse ->
            queryResponse.account.quorum
        }

    /** {@inheritDoc} */
    override fun getTransactions(hashes: Iterable<String>): Result<QryResponses.TransactionsResponse, Exception> =
        Result.of { queryAPI.getTransactions(hashes) }

    /** {@inheritDoc} */
    override fun getSingleTransaction(hash: String): Result<TransactionOuterClass.Transaction, Exception> =
        getTransactions(listOf(hash)).map { queryResponse ->
            if (queryResponse.transactionsCount == 0)
                throw Exception("There is no transactions.")

            queryResponse.transactionsList[0]
        }

    /** {@inheritDoc} */
    override fun getPeersCount(): Result<Int, Exception> = Result.of { queryAPI.peers.peersCount }

}
