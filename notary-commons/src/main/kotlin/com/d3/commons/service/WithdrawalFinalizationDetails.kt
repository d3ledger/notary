/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.service

import com.d3.commons.util.GsonInstance
import java.math.BigDecimal


/**
 * Data class that stores details of withdrawal finalization
 * @param withdrawalAmount - amount of withdrawal operation
 * @param withdrawalAssetId - asset id of withdrawal
 * @param feeAmount - amount of fee for withdrawal
 * @param feeAssetId - asset id of fee
 * @param srcAccountId - id of account that initiated current withdrawal
 * @param withdrawalTime - time of withdrawal
 * @param destinationAddress - destination address
 * @param sideChainFee - fee in sidechain(gas, mining fee, etc). null by default
 */
data class WithdrawalFinalizationDetails(
    val withdrawalAmount: BigDecimal,
    val withdrawalAssetId: String,
    val feeAmount: BigDecimal,
    val feeAssetId: String,
    val srcAccountId: String,
    val withdrawalTime: Long,
    val destinationAddress: String,
    val sideChainFee: BigDecimal? = null
) {
    /**
     * Transforms finalization details object into json string
     * @return json string
     */
    fun toJson() = GsonInstance.get().toJson(this)

    companion object {
        /**
         * Transforms given [json] into finalization details object
         * @return finalization details object
         */
        fun fromJson(json: String): WithdrawalFinalizationDetails =
            GsonInstance.get().fromJson(json, WithdrawalFinalizationDetails::class.java)
    }
}
