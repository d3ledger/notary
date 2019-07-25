package com.d3.commons.service

import com.google.gson.Gson
import java.math.BigDecimal

private val gson = Gson()

/**
 * Data class that stores details of withdrawal finalization
 * @param withdrawalAmount - amount of withdrawal operation
 * @param withdrawalAssetId - asset id of withdrawal
 * @param feeAmount - amount of fee for withdrawal
 * @param feeAssetId - asset id of fee
 * @param srcAccountId - id of account that initiated current withdrawal
 * @param withdrawalTime - time of withdrawal
 */
data class FinalizationDetails(
    val withdrawalAmount: BigDecimal,
    val withdrawalAssetId: String,
    val feeAmount: BigDecimal,
    val feeAssetId: String,
    val srcAccountId: String,
    val withdrawalTime: Long
) {
    /**
     * Transforms finalization details object into json string
     * @return json string
     */
    fun toJson() = gson.toJson(this)

    companion object {
        /**
         * Transforms given [json] into finalization details object
         * @return finalization details object
         */
        fun fromJson(json: String) =
            gson.fromJson(json, FinalizationDetails::class.java)
    }
}
