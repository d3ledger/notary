/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.provider

import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper

/**
 * Provider of clients in Notary
 */
class NotaryClientsProvider(
    private val notaryIrohaQueryHelper: IrohaQueryHelper,
    private val clientStorageAccount: String
) {

    /**
     * Checks if given [accountId] is a notary client
     * @param accountId - account id to check
     * @return true if given account id is our client
     */
    fun isClient(accountId: String) = notaryIrohaQueryHelper.keyExistsInDetails(clientStorageAccount, accountId)
}
