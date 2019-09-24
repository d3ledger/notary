/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.service

import com.d3.commons.sidechain.iroha.FEE_ROLLBACK_DESCRIPTION
import com.d3.commons.sidechain.iroha.ROLLBACK_DESCRIPTION
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import iroha.protocol.Commands
import jp.co.soramitsu.iroha.java.Transaction
import java.math.BigDecimal

/**
 * Class that returns assets in Iroha in case of failed withdrawal
 */
class RollbackService(private val withdrawalConsumer: IrohaConsumer) {

    /**
     * Return Iroha asset and fee.
     * @param withdrawalDetails - details with info about withdrawal and fee
     * @param reason - text description of rollback
     * @return hash of rollback Iroha transaction
     */
    fun rollback(
        withdrawalDetails: WithdrawalFinalizationDetails,
        reason: String
    ): Result<String, Exception> {
        return withdrawalConsumer.getConsumerQuorum()
            .map { quorum ->
                createRollbackTransaction(withdrawalDetails, reason, quorum)
            }.flatMap { transaction ->
                withdrawalConsumer.send(transaction)
            }
    }

    /**
     * Creates rollback Iroha transaction
     * @param withdrawalDetails - details with info about withdrawal and fee
     * @param reason - text description of rollback
     * @param quorum - transaction quorum
     * @return rollback Iroha transaction
     */
    fun createRollbackTransaction(
        withdrawalDetails: WithdrawalFinalizationDetails,
        reason: String,
        quorum: Int
    ): Transaction {
        val transaction = Transaction
            .builder(withdrawalConsumer.creator)
            .setCreatedTime(withdrawalDetails.withdrawalTime)
            .setQuorum(quorum)
            .transferAsset(
                withdrawalConsumer.creator,
                withdrawalDetails.srcAccountId,
                withdrawalDetails.withdrawalAssetId,
                "$ROLLBACK_DESCRIPTION. $reason".take(64).toLowerCase(),
                withdrawalDetails.withdrawalAmount
            )

        if (withdrawalDetails.feeAmount > BigDecimal.ZERO)
            transaction.transferAsset(
                withdrawalConsumer.creator,
                withdrawalDetails.srcAccountId,
                withdrawalDetails.feeAssetId,
                FEE_ROLLBACK_DESCRIPTION,
                withdrawalDetails.feeAmount
            )

        return transaction.build()
    }
}
