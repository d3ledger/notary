/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.expansion

import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.consumer.MultiSigIrohaConsumer
import com.d3.commons.util.unHex
import com.d3.commons.util.GsonInstance
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging

object ExpansionUtils : KLogging() {

    private val gson = GsonInstance.get()

    /**
     * Expansion logic that adds signature to the account passed in [expansionDetails]
     * @param irohaAPI - iroha API
     * @param mstAccount - credentials of account that adds signatories
     * @param expansionDetails - expansions details that contain account id to expand
     * @param triggerTime - time of transaction that is used in multisig
     */
    fun addSignatureExpansionLogic(
        irohaAPI: IrohaAPI,
        mstAccount: IrohaCredential,
        expansionDetails: ExpansionDetails,
        triggerTime: Long
    ) {
        if (mstAccount.accountId == expansionDetails.accountIdToExpand) {
            val consumer = MultiSigIrohaConsumer(mstAccount, irohaAPI)
            consumer.send(
                Transaction.builder(expansionDetails.accountIdToExpand)
                    .addSignatory(
                        expansionDetails.accountIdToExpand,
                        String.unHex(expansionDetails.publicKey.toLowerCase())
                    )
                    .setAccountQuorum(mstAccount.accountId, expansionDetails.quorum)
                    .setQuorum(consumer.getConsumerQuorum().get())
                    .setCreatedTime(triggerTime)
                    .build()
            ).fold(
                { hash -> logger.info { "Expansion transaction with hash $hash was sent, expansion details: $expansionDetails" } },
                { ex -> throw ex }
            )
        }
    }

    /**
     * Creates transaction that triggers the expansion process
     *
     * @param creatorAccountId - creator of trigger transaction
     * @param expansionDetails - details of expansion
     * @param triggerAccountId - account that is used as a trigger
     * @return unsigned transaction
     */
    fun createExpansionTriggerTx(
        creatorAccountId: String,
        expansionDetails: ExpansionDetails,
        triggerAccountId: String
    ): Transaction {
        return Transaction
            .builder(creatorAccountId)
            .setAccountDetail(
                triggerAccountId,
                System.currentTimeMillis().toString(),
                Utils.irohaEscape(gson.toJson(expansionDetails))
            ).build()
    }
}
