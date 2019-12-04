/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.exchange.exchanger.strategy

import com.google.gson.JsonParser
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Rate strategy based on http querying of data collector service
 */
class DcRateStrategy(
    private val baseRateUrl: String,
    private val baseAssetId: String,
    private val rateAttribute: String,
    feeFraction: BigDecimal
) : RateStrategy(feeFraction) {

    private val parser = JsonParser()

    override fun getAmount(from: String, to: String, amount: BigDecimal): BigDecimal {
        val fromRate = getRateOrBaseAsset(from)
        val toRate = getRateOrBaseAsset(to)
        val amountWithRespectToFee = getAmountWithRespectToFee(amount)
        return toRate
            .multiply(amountWithRespectToFee)
            .divide(
                fromRate,
                MAX_PRECISION,
                RoundingMode.HALF_DOWN
            )
    }

    private fun getRateOrBaseAsset(assetId: String) =
        if (assetId == baseAssetId) {
            BigDecimal.ONE
        } else {
            getRateFor(assetId)
        }

    /**
     * Queries dc and parses its response
     */
    private fun getRateFor(assetId: String): BigDecimal {
        val nameDomain = assetId.split("#")
        val response = khttp.get("$baseRateUrl/${nameDomain[0]}/${nameDomain[1]}")
        if (response.statusCode != 200) {
            throw IllegalStateException("Couldn't query data collector, response: ${response.text}")
        }
        val jsonElement = parser.parse(response.text).asJsonObject.get(rateAttribute)
        if (jsonElement.isJsonNull) {
            throw IllegalStateException("Asset not found in data collector")
        }
        return BigDecimal(jsonElement.asString)
    }

    companion object {
        const val MAX_PRECISION = 18
    }
}
