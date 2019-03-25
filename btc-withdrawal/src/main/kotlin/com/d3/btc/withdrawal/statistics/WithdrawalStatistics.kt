/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.btc.withdrawal.statistics

import java.util.concurrent.atomic.AtomicInteger

/**
 * Data class that holds short statistics about withdrawal service
 */
data class WithdrawalStatistics(
    val totalTransfers: AtomicInteger,
    val failedTransfers: AtomicInteger,
    val succeededTransfers: AtomicInteger
) {
    fun incTotalTransfers() = totalTransfers.incrementAndGet()

    fun incFailedTransfers() = failedTransfers.incrementAndGet()

    fun incSucceededTransfers() = succeededTransfers.incrementAndGet()

    companion object {
        fun create() = WithdrawalStatistics(AtomicInteger(), AtomicInteger(), AtomicInteger())
    }
}
