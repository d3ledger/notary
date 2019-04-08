package com.d3.btc.fee

import java.util.concurrent.atomic.AtomicInteger

private const val MIN_FEE_RATE = 10

/**
 * Singleton object that holds information abount current fee rate
 */
object CurrentFeeRate {
    private const val NOT_SET_FEE_RATE = -1

    private var feeRate = AtomicInteger(NOT_SET_FEE_RATE)
    /**
     * Checks if fee rate was set
     */
    fun isPresent() = feeRate.get() > NOT_SET_FEE_RATE

    /**
     * Returns current fee rate
     */
    fun get() = Math.max(feeRate.get(), MIN_FEE_RATE)

    /**
     * Sets fee rate
     * @param feeRate - fee rate
     */
    fun set(feeRate: Int) = this.feeRate.set(feeRate)

    /**
     * Sets minimum fee rate
     */
    fun setMinimum() = set(MIN_FEE_RATE)

    /**
     * Clears current fee rate.
     * After this call fee rate will be considered as 'no set'
     */
    fun clear() = set(NOT_SET_FEE_RATE)

}
