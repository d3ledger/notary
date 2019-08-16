/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.exchange

import com.d3.exchange.exchanger.strategy.CurveRateStrategy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrateTest {

    private val SOURCE_BALANCE = 200.0
    private val TARGET_BALANCE = 100.0
    private val SUPPLY = 3.0
    private val FEE = 0.01

    /**
     * Checks the integrating formula correctness
     */
    @Test
    fun integrateTest() {
        val resultAmount =
            CurveRateStrategy.integrate(SOURCE_BALANCE, TARGET_BALANCE, SUPPLY * (1 - FEE))
        // Invariant
        assertTrue((SOURCE_BALANCE + SUPPLY) * (TARGET_BALANCE - resultAmount) > SOURCE_BALANCE * TARGET_BALANCE)
        // Can be false for huge amount
        assertTrue(resultAmount > (SUPPLY - 1) * TARGET_BALANCE / SOURCE_BALANCE)
        // Not fixed rate
        assertTrue(resultAmount < SUPPLY * TARGET_BALANCE / SOURCE_BALANCE)
    }
}
