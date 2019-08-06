/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.service

import com.d3.commons.sidechain.iroha.ROLLBACK_DESCRIPTION
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.Transaction
import mu.KLogging
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
                val transaction = Transaction
                    .builder(withdrawalConsumer.creator, withdrawalDetails.withdrawalTime)
                    .setQuorum(quorum)
                    .setCreatedTime(withdrawalDetails.withdrawalTime)
                    .transferAsset(
                        withdrawalDetails.destinationAddress,
                        withdrawalDetails.srcAccountId,
                        withdrawalDetails.withdrawalAssetId,
                        "$ROLLBACK_DESCRIPTION. $reason".take(64).toLowerCase(),
                        withdrawalDetails.withdrawalAmount
                    )

                if (withdrawalDetails.feeAmount > BigDecimal.ZERO)
                    transaction.transferAsset(
                        withdrawalDetails.destinationAddress,
                        withdrawalDetails.srcAccountId,
                        withdrawalDetails.feeAssetId,
                        "$ROLLBACK_DESCRIPTION. $reason".take(64).toLowerCase(),
                        withdrawalDetails.feeAmount
                    )

                transaction.build()
            }.flatMap { transaction ->
                withdrawalConsumer.send(transaction)
            }
    }
}
