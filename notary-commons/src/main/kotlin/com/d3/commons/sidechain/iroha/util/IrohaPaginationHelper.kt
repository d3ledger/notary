/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.iroha.util

import com.d3.commons.util.GsonInstance
import com.github.kittinunf.result.Result
import com.google.gson.reflect.TypeToken
import jp.co.soramitsu.iroha.java.QueryAPI
import java.util.*
import kotlin.collections.HashMap

/**
 * Class that is used to filter paginated data from Iroha
 * @param queryAPI - query API by Iroha-Java library
 * @param pageSize - number of items per one Iroha query.
 */
class IrohaPaginationHelper(private val queryAPI: QueryAPI, private val pageSize: Int) {

    private val gson = GsonInstance.get()

    /**
     * Returns all account details
     * @param storageAccountId - details holder account id
     * @param writerAccountId - details setter account id
     * @param key - detail key
     * @return all account details
     */
    fun getPaginatedAccountDetails(
        storageAccountId: String,
        writerAccountId: String?,
        key: String?
    ): Result<Map<String, Map<String, String>>, Exception> = Result.of {
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

    /**
     * Returns filtered account details
     * @param storageAccountId - details holder account id
     * @param writerAccountId - details setter account id
     * @param filterPredicate - details filter
     * @return filtered account details
     */
    fun getPaginatedAccountDetailsFilter(
        storageAccountId: String,
        writerAccountId: String,
        filterPredicate: (key: String, value: String) -> Boolean
    ) = Result.of {
        var lastWriter: String? = null
        var lastKey: String? = null
        val detailsMap = HashMap<String, String>()
        do {
            val response = queryAPI.getAccountDetails(
                storageAccountId,
                writerAccountId,
                null,
                pageSize,
                lastWriter,
                lastKey
            )
            lastWriter = response.nextRecordId.writer
            lastKey = response.nextRecordId.key
            val filteredDetails =
                parseAccountDetailsJson(response.detail).get()
                    .getOrDefault(writerAccountId, emptyMap())
                    .filter { entry -> filterPredicate(entry.key, entry.value) }
            detailsMap.putAll(filteredDetails)
        } while (response.hasNextRecordId())
        detailsMap
    }

    /**
     * Returns the first account detail complied to a given predicate
     * @param storageAccountId - details holder account id
     * @param writerAccountId - details setter account id
     * @param shufflePage - flag that makes this function to shuffle page result before applying any predicate
     * @param firstPredicate - predicate
     * @return the first account detail complied to [firstPredicate] or null
     */
    fun getPaginatedAccountDetailsFirst(
        storageAccountId: String,
        writerAccountId: String,
        shufflePage: Boolean,
        firstPredicate: (key: String, value: String) -> Boolean
    ) = Result.of {
        var lastWriter: String? = null
        var lastKey: String? = null
        do {
            val response = queryAPI.getAccountDetails(
                storageAccountId,
                writerAccountId,
                null,
                pageSize,
                lastWriter,
                lastKey
            )
            lastWriter = response.nextRecordId.writer
            lastKey = response.nextRecordId.key
            var pageDetails: Iterable<Map.Entry<String, String>> = parseAccountDetailsJson(response.detail).get()
                .getOrDefault(writerAccountId, emptyMap())
                .entries
            if (shufflePage) {
                pageDetails = pageDetails.shuffled()
            }
            val firstDetails = pageDetails.firstOrNull { entry -> firstPredicate(entry.key, entry.value) }
            if (firstDetails != null) {
                return@of Optional.of<Map.Entry<String, String>>(
                    AbstractMap.SimpleEntry(
                        firstDetails.key,
                        firstDetails.value
                    )
                )
            }
        } while (response.hasNextRecordId())
        return@of Optional.empty<Map.Entry<String, String>>()
    }

    /**
     * Returns the number of account details complied to a given predicate
     * @param storageAccountId - details holder account id
     * @param writerAccountId - details setter account id
     * @param countPredicate - predicate
     * @return the number of account details complied to [countPredicate]
     */
    fun getPaginatedAccountDetailsCount(
        storageAccountId: String,
        writerAccountId: String,
        countPredicate: (key: String, value: String) -> Boolean
    ) = Result.of {
        var counter = 0
        var lastWriter: String? = null
        var lastKey: String? = null
        do {
            val response = queryAPI.getAccountDetails(
                storageAccountId,
                writerAccountId,
                null,
                pageSize,
                lastWriter,
                lastKey
            )
            lastWriter = response.nextRecordId.writer
            lastKey = response.nextRecordId.key
            counter +=
                parseAccountDetailsJson(response.detail).get()
                    .getOrDefault(writerAccountId, emptyMap())
                    .entries.count { entry -> countPredicate(entry.key, entry.value) }

        } while (response.hasNextRecordId())
        counter
    }

    fun getPaginatedAccountAssets(
        accountId: String
    ) = Result.of {
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

    /**
     * Deserialize JSON string to Map<String, String>
     *
     * @param jsonStr JSON as String
     * @return desereialized details as (writer -> (key -> value))
     */
    private fun parseAccountDetailsJson(jsonStr: String): Result<Map<String, Map<String, String>>, Exception> =
        Result.of {
            val responseType = object : TypeToken<Map<String, Map<String, String>>>() {}.type
            gson.fromJson<Map<String, Map<String, String>>>(jsonStr, responseType)
        }
}
