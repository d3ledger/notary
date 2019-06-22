/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.expansion

/**
 * Details of expansion process
 *
 * @param accountIdToExpand - accountId to expand
 * @param publicKey - new public key
 * @param quorum - new quorum
 * @param additionalData - extra data like Ethereum keys
 */
class ExpansionDetails(
    val accountIdToExpand: String,
    val publicKey: String,
    val quorum: Int,
    val additionalData: Map<String, String>
) {

    override fun toString(): String {
        return "ExpansionDetails{" +
                "accountIdToExpand='" + accountIdToExpand + '\''.toString() +
                ", publicKey='" + publicKey + '\''.toString() +
                ", quorum=" + quorum +
                ", additionalData=" + additionalData +
                '}'.toString()
    }
}
