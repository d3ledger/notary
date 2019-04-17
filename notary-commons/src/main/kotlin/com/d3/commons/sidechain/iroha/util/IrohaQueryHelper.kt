/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.iroha.util

import com.github.kittinunf.result.Result

interface IrohaQueryHelper {

    fun getAccountDetails(
        storageAccountId: String,
        writerAccountId: String
    ): Result<Map<String, String>, Exception>

    fun getAssetPrecision(assetId: String): Result<Int, Exception>
}
