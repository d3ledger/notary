/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.provider

import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.d3.notifications.client.D3Client
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map

/**
 * Class that is used to get information about D3 clients
 */
class D3ClientProvider(private val notaryQueryHelper: IrohaQueryHelper) {

    /**
     * Returns D3 client by its id
     * @param accountId - account id of client
     * @return D3 client
     */
    fun getClient(accountId: String): Result<D3Client, Exception> {
        // Assuming that client sets details himself
        return notaryQueryHelper.getAccountDetails(accountId, accountId)
            .map { accountDetails ->
                D3Client.create(accountId, accountDetails)
            }
    }
}
