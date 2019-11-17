/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.iroha.util.impl

import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.github.kittinunf.result.Result
import io.grpc.Status
import io.grpc.StatusRuntimeException
import mu.KLogging

private const val TIMEOUT_MLS = 5_000L
private const val MIN_TIMEOUT = TIMEOUT_MLS * 2

/**
 * Class that wraps [IrohaQueryHelperImpl]. Unlike [IrohaQueryHelperImpl] it tries to query Iroha multiple times if Iroha is off-line.
 * @param irohaQueryHelper - [IrohaQueryHelperImpl] instance to wrap
 * @param totalTimeoutMls - max time to retry in milliseconds.
 * In fact it will be in range (totalTimeoutMls-TIMEOUT_MLS ; totalTimeoutMls)
 */
class RobustIrohaQueryHelperImpl(
    private val irohaQueryHelper: IrohaQueryHelperImpl,
    private val totalTimeoutMls: Int
) : IrohaQueryHelper {

    init {
        if (totalTimeoutMls <= MIN_TIMEOUT) {
            throw IllegalArgumentException("'totalTimeoutMls' value must be bigger than $MIN_TIMEOUT")
        }
    }

    override fun getQueryCreatorAccountId() = irohaQueryHelper.getQueryCreatorAccountId()

    override fun getAccountDetailsFirst(
        storageAccountId: String,
        writerAccountId: String,
        firstPredicate: (key: String, value: String) -> Boolean
    ) = retryQuery { irohaQueryHelper.getAccountDetailsFirst(storageAccountId, writerAccountId, firstPredicate) }

    override fun getAccountDetailsFirstShufflePage(
        storageAccountId: String,
        writerAccountId: String,
        firstPredicate: (key: String, value: String) -> Boolean
    ) = retryQuery {
        irohaQueryHelper.getAccountDetailsFirstShufflePage(
            storageAccountId,
            writerAccountId,
            firstPredicate
        )
    }

    override fun getAccountDetailsByKeyOnly(storageAccountId: String, key: String) = retryQuery {
        irohaQueryHelper.getAccountDetailsByKeyOnly(storageAccountId, key)
    }

    override fun getAccountDetailsFilter(
        storageAccountId: String,
        writerAccountId: String,
        filterPredicate: (key: String, value: String) -> Boolean
    ) = retryQuery { irohaQueryHelper.getAccountDetailsFilter(storageAccountId, writerAccountId, filterPredicate) }

    override fun getAccountDetailsCount(
        storageAccountId: String,
        writerAccountId: String,
        countPredicate: (key: String, value: String) -> Boolean
    ) = retryQuery { irohaQueryHelper.getAccountDetailsCount(storageAccountId, writerAccountId, countPredicate) }

    override fun getAccount(accountId: String) = retryQuery { irohaQueryHelper.getAccount(accountId) }

    override fun getSignatories(accountId: String) = retryQuery { irohaQueryHelper.getSignatories(accountId) }

    override fun getAccountDetails(
        storageAccountId: String,
        writerAccountId: String
    ) = retryQuery { irohaQueryHelper.getAccountDetails(storageAccountId, writerAccountId) }

    override fun getAccountDetails(
        storageAccountId: String,
        writerAccountId: String,
        key: String
    ) = retryQuery { irohaQueryHelper.getAccountDetails(storageAccountId, writerAccountId, key) }

    override fun getAssetPrecision(assetId: String) = retryQuery { irohaQueryHelper.getAssetPrecision(assetId) }

    override fun getAccountAssets(accountId: String) = retryQuery { irohaQueryHelper.getAccountAssets(accountId) }

    override fun getAccountAsset(accountId: String, assetId: String) =
        retryQuery { irohaQueryHelper.getAccountAsset(accountId, assetId) }

    override fun getBlock(height: Long) = retryQuery { irohaQueryHelper.getBlock(height) }

    override fun getAccountQuorum(acc: String) = retryQuery { irohaQueryHelper.getAccountQuorum(acc) }

    override fun getTransactions(hashes: Iterable<String>) = retryQuery { irohaQueryHelper.getTransactions(hashes) }

    override fun getSingleTransaction(hash: String) = retryQuery { irohaQueryHelper.getSingleTransaction(hash) }

    override fun getPeersCount() = retryQuery { irohaQueryHelper.getPeersCount() }

    override fun isRegistered(
        accountName: String,
        domainId: String,
        publicKey: String
    ): Result<Boolean, Exception> =
        retryQuery { irohaQueryHelper.isRegistered(accountName, domainId, publicKey) }

    /**
     * Queries Iroha until it responds
     * @param attempt - attempt counter. 0 by default
     * @param startTime - operation start time. Current time by default
     * @param timeToWait - time to wait
     * @param queryCall - query function to call
     * @return result of the query or [RobustQueryException] if we reached the timeout
     */
    private fun <T : Any> retryQuery(
        attempt: Int = 0,
        startTime: Long = System.currentTimeMillis(),
        queryCall: () -> Result<T, Exception>
    ): Result<T, Exception> {
        // Make query
        val result = queryCall()
        return result.fold(
            {
                // Return result if everything is ok
                return result
            },
            { ex ->
                if (isIrohaOnline(ex)) {
                    // Return result if Iroha is online
                    return result
                } else if (noSenseToWait(startTime)) {
                    // Do not wait and return an error if we cannot wait to re-queue anymore
                    return Result.error(RobustQueryException(attempt + 1))
                }
                logger.warn("Cannot query Iroha. Wait and retry. Attempt ${attempt + 1}")
                // Wait a little
                Thread.sleep(TIMEOUT_MLS)
                // Go recursive
                return retryQuery(
                    attempt = attempt + 1,
                    startTime = startTime,
                    queryCall = queryCall
                )
            })
    }

    /**
     * Check if there is no sense to wait anymore
     * @param startTime - operation start time
     * @return true if there is no sense to wait
     */
    private fun noSenseToWait(startTime: Long): Boolean {
        val totalTimePassed = System.currentTimeMillis() - startTime
        return totalTimePassed + TIMEOUT_MLS > totalTimeoutMls
    }

    /**
     * Checks if Iroha is online based on [error]
     * @param error - error to check
     * @return true if Iroha is online.
     */
    private fun isIrohaOnline(error: Exception) =
        !(error is StatusRuntimeException && error.status.code == Status.Code.UNAVAILABLE)

    companion object : KLogging()
}

class RobustQueryException(val retryAttempts: Int) :
    Exception("Iroha is unavailable. Cannot query. Query attempts $retryAttempts")
