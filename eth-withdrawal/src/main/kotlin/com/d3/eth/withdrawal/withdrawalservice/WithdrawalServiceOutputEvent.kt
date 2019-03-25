/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.withdrawal.withdrawalservice

/**
 * Events are emitted by withdrawal service
 */
sealed class WithdrawalServiceOutputEvent {

    /**
     * Refund in Ethereum chain
     */
    data class EthRefund(val proof: RollbackApproval) : WithdrawalServiceOutputEvent()
}
