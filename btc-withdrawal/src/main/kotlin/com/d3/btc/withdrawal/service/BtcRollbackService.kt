/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.btc.withdrawal.service

import com.d3.btc.helper.currency.satToBtc
import com.d3.btc.withdrawal.transaction.WithdrawalDetails
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.github.kittinunf.result.flatMap
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

private const val BTC_ASSET_ID = "btc#bitcoin"

/**
 * Bitcoin rollback service
 */
@Component
class BtcRollbackService(
    @Qualifier("withdrawalConsumer")
    @Autowired private val withdrawalConsumer: IrohaConsumer
) {

    /**
     * Rollbacks given amount of money to a particular Iroha account
     * @param withdrawalDetails - details of withdrawal to rollback
     * @param reason - reason of rollback
     */
    fun rollback(withdrawalDetails: WithdrawalDetails, reason: String) {
        rollback(
            withdrawalDetails.sourceAccountId,
            withdrawalDetails.amountSat,
            withdrawalDetails.withdrawalTime,
            reason
        )
    }

    /**
     * Rollbacks given amount of money to a particular Iroha account
     * @param accountId - Iroha account id, which money will be restored
     * @param amountSat - amount of money to rollback in SAT format
     * @param withdrawalTime - time of withdrawal that needs a rollback. Used in multisig.
     * @param reason - reason of rollback
     */
    fun rollback(accountId: String, amountSat: Long, withdrawalTime: Long, reason: String) {
        withdrawalConsumer.getConsumerQuorum().flatMap { quorum ->
            ModelUtil.transferAssetIroha(
                withdrawalConsumer,
                withdrawalConsumer.creator,
                accountId,
                BTC_ASSET_ID,
                "Rollback. $reason",
                satToBtc(amountSat).toPlainString(),
                withdrawalTime,
                quorum
            )
        }.fold(
            {
                logger.info {
                    "Rollback(accountId:$accountId, amount:${satToBtc(amountSat).toPlainString()}) was committed"
                }
            },
            { ex -> logger.error("Cannot perform rollback", ex) })
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
