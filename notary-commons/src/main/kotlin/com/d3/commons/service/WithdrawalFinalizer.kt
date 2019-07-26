/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.service

import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.util.irohaEscape
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import jp.co.soramitsu.iroha.java.Transaction
import java.math.BigDecimal

const val LAST_SUCCESSFUL_WITHDRAWAL_KEY = "last_successful_withdrawal"

/**
 * Class that is used to finalize withdrawals
 */
class WithdrawalFinalizer(
    private val withdrawalIrohaConsumer: IrohaConsumer,
    private val billingAccount: String
) {

    /**
     * Finalizes withdrawal
     * @param withdrawalFinalizationDetails - details of finalization
     * @return hash of transaction if successful
     */
    fun finalize(withdrawalFinalizationDetails: WithdrawalFinalizationDetails): Result<String, Exception> {
        return withdrawalIrohaConsumer.getConsumerQuorum().flatMap { quorum ->
            withdrawalIrohaConsumer.send(createFinalizeTransaction(withdrawalFinalizationDetails, quorum))
        }
    }

    /**
     * Creates transaction that finalizes withdrawal operation
     * @param withdrawalFinalizationDetails - details of finalization
     * @return transaction
     */
    private fun createFinalizeTransaction(withdrawalFinalizationDetails: WithdrawalFinalizationDetails, quorum: Int): Transaction {
        val transactionBuilder = Transaction.builder(withdrawalIrohaConsumer.creator)
        if (withdrawalFinalizationDetails.feeAmount > BigDecimal.ZERO) {
            // Pay fees to the corresponding account
            transactionBuilder.transferAsset(
                withdrawalIrohaConsumer.creator,
                billingAccount,
                withdrawalFinalizationDetails.feeAssetId,
                "Fee",
                withdrawalFinalizationDetails.feeAmount
            )
        }
        // Set last successful withdrawal
        transactionBuilder.setAccountDetail(
            withdrawalIrohaConsumer.creator,
            LAST_SUCCESSFUL_WITHDRAWAL_KEY,
            withdrawalFinalizationDetails.toJson().irohaEscape()
        )
        // Burn withdrawal account money to keep 2WP consistent
        transactionBuilder
            .subtractAssetQuantity(withdrawalFinalizationDetails.withdrawalAssetId, withdrawalFinalizationDetails.withdrawalAmount)
            .setCreatedTime(withdrawalFinalizationDetails.withdrawalTime)
            .setQuorum(quorum)
        return transactionBuilder.build()
    }
}
