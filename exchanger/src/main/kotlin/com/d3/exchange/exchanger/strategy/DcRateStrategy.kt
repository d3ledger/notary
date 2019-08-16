/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.exchange.exchanger.strategy

import com.google.gson.JsonParser
import java.math.BigDecimal

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
        val fromRate: BigDecimal =
            if (from == baseAssetId) {
                BigDecimal.ONE
            } else {
                getRateFor(from)
            }
        val toRate: BigDecimal =
            if (to == baseAssetId) {
                BigDecimal.ONE
            } else {
                getRateFor(to)
            }
        return fromRate.divide(toRate).multiply(amount.multiply(feeFraction))
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
}
