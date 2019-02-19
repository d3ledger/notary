package com.d3.btc.withdrawal.transaction

import org.bitcoinj.core.Transaction

/**
 * Withdrawal transaction
 */
data class WithdrawalTx(val withdrawalDetails: WithdrawalDetails, val tx: Transaction)

/**
 * Withdrawal details
 * @param sourceAccountId - account that commits withdrawal
 * @param amountSat - desired amount of SAT to withdraw
 * @param withdrawalTime - time of withdrawal
 */
data class WithdrawalDetails(val sourceAccountId: String, val amountSat: Long, val withdrawalTime: Long)
