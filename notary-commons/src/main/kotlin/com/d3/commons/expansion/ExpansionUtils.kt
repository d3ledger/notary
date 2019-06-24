/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.expansion

import com.google.gson.Gson
import jp.co.soramitsu.bootstrap.changelog.ExpansionDetails
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.Utils

object ExpansionUtils {

    private val gson = Gson()

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
