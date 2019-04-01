package com.d3.btc.withdrawal.transaction

import org.bitcoinj.core.Transaction
import kotlin.math.absoluteValue

/**
 * Withdrawal transaction
 */
data class WithdrawalTx(val withdrawalDetails: WithdrawalDetails, val tx: Transaction)

/**
 * Withdrawal details
 * @param sourceAccountId - account that commits withdrawal
 * @param amountSat - desired amount of SAT to withdraw
 * @param toAddress - Bitcoin destination address in base58 format
 * @param withdrawalTime - time of withdrawal
 */
data class WithdrawalDetails(
    val sourceAccountId: String,
    val toAddress: String,
    val amountSat: Long,
    val withdrawalTime: Long
) {
    fun irohaFriendlyHashCode() = hashCode().absoluteValue.toString()
}
