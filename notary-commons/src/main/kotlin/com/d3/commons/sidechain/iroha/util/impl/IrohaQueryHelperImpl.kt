/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.iroha.util.impl

import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.github.kittinunf.result.Result
import jp.co.soramitsu.iroha.java.QueryAPI

/**
 * The purpose of the class is to hide Iroha query implementation.
 */
class IrohaQueryHelperImpl(val queryAPI: QueryAPI) : IrohaQueryHelper {

    override fun getAccountDetails(
        storageAccountId: String,
        writerAccountId: String
    ): Result<Map<String, String>, Exception> {
        return com.d3.commons.sidechain.iroha.util.getAccountDetails(
            queryAPI,
            storageAccountId,
            writerAccountId
        )
    }

    override fun getAssetPrecision(assetId: String): Result<Int, Exception> {
        return com.d3.commons.sidechain.iroha.util.getAssetPrecision(queryAPI, assetId)
    }

}
