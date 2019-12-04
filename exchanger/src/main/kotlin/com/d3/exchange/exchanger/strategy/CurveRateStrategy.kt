/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.exchange.exchanger.strategy

import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.d3.exchange.exchanger.exceptions.TooMuchAssetVolumeException
import mu.KLogging
import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.integration.RombergIntegrator
import java.math.BigDecimal

/**
 * Rate strategy based on corresponding asset balances curve integration
 */
class CurveRateStrategy(
    private val exchangerAccountId: String,
    private val queryhelper: IrohaQueryHelper,
    feeFraction: BigDecimal
) : RateStrategy(feeFraction) {

    /**
     * Uses integration to calculate how much assets should be sent back to the client
     * Logic is not to fix a rate in a moment but to determine continuous rate for
     * any amount of assets.
     * @throws TooMuchAssetVolumeException in case of impossible conversion when supplied too much
     */
    override fun getAmount(from: String, to: String, amount: BigDecimal): BigDecimal {
        val sourceAssetBalance = BigDecimal(
            queryhelper.getAccountAsset(exchangerAccountId, from).get()
        ).minus(amount).toDouble()
        val targetAssetBalance =
            BigDecimal(queryhelper.getAccountAsset(exchangerAccountId, to).get())

        val calculatedAmount = BigDecimal(
            integrate(
                sourceAssetBalance,
                targetAssetBalance.toDouble(),
                getAmountWithRespectToFee(amount).toDouble()
            )
        )

        if (calculatedAmount >= targetAssetBalance) {
            throw TooMuchAssetVolumeException("Asset supplement exceeds the balance.")
        }

        return calculatedAmount
    }

    companion object : KLogging() {
        // Number of evaluations during integration
        private const val EVALUATIONS = 1000
        // Integrating from the relevant rate which is at x=0
        private const val LOWER_BOUND = 0.0

        // Integrals
        private val integrator = RombergIntegrator()

        /**
         * Performs integration of target asset amount function
         */
        fun integrate(
            sourceAssetBalance: Double,
            targetAssetBalance: Double,
            amount: Double
        ): Double {
            val function =
                UnivariateFunction { x -> targetAssetBalance / (sourceAssetBalance + x + 1) }
            return integrator.integrate(
                EVALUATIONS, function,
                LOWER_BOUND, LOWER_BOUND + amount
            )
        }
    }
}
