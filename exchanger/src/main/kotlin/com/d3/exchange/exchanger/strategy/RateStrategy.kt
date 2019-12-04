/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.exchange.exchanger.strategy

import java.math.BigDecimal

/**
 * Amount retrieval/calculation interface
 */
abstract class RateStrategy(
    private val feeFraction: BigDecimal
) {

    /**
     * Method for calculating relevant exchange value for assets
     */
    abstract fun getAmount(from: String, to: String, amount: BigDecimal): BigDecimal

    fun getAmountWithRespectToFee(amount: BigDecimal) = amount.minus(amount.multiply(feeFraction))
}
