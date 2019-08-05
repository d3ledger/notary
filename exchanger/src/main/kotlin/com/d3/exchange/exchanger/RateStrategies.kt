/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.exchange.exchanger

import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Rate retrieval interface
 */
interface RateStrategy {

    /**
     * Method for retrieving relevant exchange rate for 'non standard' assets
     */
    fun getRate(from: String, to: String): BigDecimal
}

@Component
class DcRateStrategy : RateStrategy {
    override fun getRate(from: String, to: String): BigDecimal {
        TODO("not implemented")
    }
}