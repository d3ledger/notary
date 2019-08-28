/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.provider

import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map



/**
 * Provider of clients in Notary
 */
class NotaryClientsProvider(
    private val notaryIrohaQueryHelper: IrohaQueryHelper,
    private val clientStorageAccount: String,
    private val registrationServiceAccountName:String
) {

    /**
     * Checks if given [accountId] is a notary client
     * @param accountId - account id to check
     * @return true if given account id is our client
     */
    fun isClient(accountId: String): Result<Boolean, Exception> {
        val accountName = accountId.substringBefore("@")
        val domain = accountId.substringAfter("@")
        return notaryIrohaQueryHelper.getAccountDetails(
            clientStorageAccount,
            key = "$accountName$domain",
            writerAccountId = "$registrationServiceAccountName@$domain"
        ).map { value ->
            value.isPresent && value.get() == domain
        }
    }
}
