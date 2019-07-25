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
     * @param finalizationDetails - details of finalization
     * @return hash of transaction if successful
     */
    fun finalize(finalizationDetails: FinalizationDetails): Result<String, Exception> {
        return withdrawalIrohaConsumer.getConsumerQuorum().flatMap { quorum ->
            withdrawalIrohaConsumer.send(createFinalizeTransaction(finalizationDetails, quorum))
        }
    }

    /**
     * Creates transaction that finalizes withdrawal operation
     * @param finalizationDetails - details of finalization
     * @return transaction
     */
    private fun createFinalizeTransaction(finalizationDetails: FinalizationDetails, quorum: Int): Transaction {
        val transactionBuilder = Transaction.builder(withdrawalIrohaConsumer.creator)
        if (finalizationDetails.feeAmount > BigDecimal.ZERO) {
            // Pay fees to the corresponding account
            transactionBuilder.transferAsset(
                withdrawalIrohaConsumer.creator,
                billingAccount,
                finalizationDetails.feeAssetId,
                "Fee",
                finalizationDetails.feeAmount
            )
        }
        // Set last successful withdrawal
        transactionBuilder.setAccountDetail(
            withdrawalIrohaConsumer.creator,
            LAST_SUCCESSFUL_WITHDRAWAL_KEY,
            finalizationDetails.toJson().irohaEscape()
        )
        // Burn withdrawal account money to keep 2WP consistent
        transactionBuilder
            .subtractAssetQuantity(finalizationDetails.withdrawalAssetId, finalizationDetails.withdrawalAmount)
            .setCreatedTime(finalizationDetails.withdrawalTime)
            .setQuorum(quorum)
        return transactionBuilder.build()
    }
}
