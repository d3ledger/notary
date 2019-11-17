/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.iroha.util.impl

import com.d3.commons.config.IrohaCredentialRawConfig
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.util.IrohaPaginationHelper
import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import iroha.protocol.QryResponses
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.ErrorResponseException
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import java.security.KeyPair
import java.util.*
import kotlin.collections.HashMap

/**
 * The purpose of the class is to hide Iroha query implementation.
 * @param queryAPI - query API by Iroha-Java library
 * @param irohaPaginationHelper - Iroha pagination helper. Used to filter account details and such.
 */
open class IrohaQueryHelperImpl(
    private val queryAPI: QueryAPI,
    private val irohaPaginationHelper: IrohaPaginationHelper = IrohaPaginationHelper(queryAPI, 100)
) : IrohaQueryHelper {

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

    /** {@inheritDoc} */
    override fun getQueryCreatorAccountId() = queryAPI.accountId

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
        irohaPaginationHelper.getPaginatedAccountDetails(storageAccountId, writerAccountId, null)
            .map { details ->
                if (details[writerAccountId] == null)
                    emptyMap()
                else
                    details.getOrDefault(writerAccountId, emptyMap())
            }

    /** {@inheritDoc} */
    override fun getAccountDetailsByKeyOnly(
        storageAccountId: String,
        key: String
    ): Result<Map<String, String>, Exception> {
        return irohaPaginationHelper.getPaginatedAccountDetails(storageAccountId, null, key)
            .map { details ->
                if (details.isEmpty()) {
                    HashMap()
                } else {
                    val result = HashMap<String, String>()
                    details.entries.forEach { accountLevelDetails ->
                        val setter = accountLevelDetails.key
                        accountLevelDetails.value.values.forEach { value -> result[setter] = value }
                    }
                    result
                }
            }
    }

    /** {@inheritDoc} */
    override fun getAccountDetailsFirst(
        storageAccountId: String,
        writerAccountId: String,
        firstPredicate: (key: String, value: String) -> Boolean
    ): Result<Optional<Map.Entry<String, String>>, Exception> =
        irohaPaginationHelper.getPaginatedAccountDetailsFirst(
            storageAccountId,
            writerAccountId,
            shufflePage = false,
            firstPredicate = firstPredicate
        )

    /** {@inheritDoc} */
    override fun getAccountDetailsFirstShufflePage(
        storageAccountId: String,
        writerAccountId: String,
        firstPredicate: (key: String, value: String) -> Boolean
    ): Result<Optional<Map.Entry<String, String>>, Exception> =
        irohaPaginationHelper.getPaginatedAccountDetailsFirst(
            storageAccountId,
            writerAccountId,
            shufflePage = true,
            firstPredicate = firstPredicate
        )

    /** {@inheritDoc} */
    override fun getAccountDetailsFilter(
        storageAccountId: String,
        writerAccountId: String,
        filterPredicate: (key: String, value: String) -> Boolean
    ): Result<Map<String, String>, Exception> =
        irohaPaginationHelper.getPaginatedAccountDetailsFilter(storageAccountId, writerAccountId, filterPredicate)

    /** {@inheritDoc} */
    override fun getAccountDetailsCount(
        storageAccountId: String,
        writerAccountId: String,
        countPredicate: (key: String, value: String) -> Boolean
    ): Result<Int, Exception> =
        irohaPaginationHelper.getPaginatedAccountDetailsCount(storageAccountId, writerAccountId, countPredicate)

    /** {@inheritDoc} */
    override fun getAccountDetails(
        storageAccountId: String,
        writerAccountId: String,
        key: String
    ): Result<Optional<String>, Exception> =
        irohaPaginationHelper.getPaginatedAccountDetails(storageAccountId, writerAccountId, key)
            .map { details ->
                Optional.ofNullable(details.getOrDefault(writerAccountId, emptyMap())[key])
            }

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
        irohaPaginationHelper.getPaginatedAccountAssets(accountId)

    /** {@inheritDoc} */
    override fun getAccountAsset(accountId: String, assetId: String): Result<String, Exception> =
        irohaPaginationHelper.getPaginatedAccountAssets(accountId)
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

    /** {@inheritDoc} */
    override fun isRegistered(
        accountName: String,
        domainId: String,
        publicKey: String
    ): Result<Boolean, Exception> = Result.of {
        getSignatories("$accountName@$domainId")
            .fold(
                { signatories ->
                    if (signatories.map { it.toLowerCase() }.contains(publicKey.toLowerCase())) {
                        // user with publicKey is already registered
                        true
                    } else {
                        // user is registered with a different pubkey - stateful invalid
                        throw Exception("$accountName@$domainId is registered with pubkey different from $publicKey")
                    }
                },
                {
                    // user not found
                    if (it is ErrorResponseException && it.errorResponse.errorCode == 0)
                        false
                    else
                        throw it
                }
            )
    }

}
