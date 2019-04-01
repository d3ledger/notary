package com.d3.notifications.provider

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.QueryAPI
import com.d3.notifications.client.D3Client
import com.d3.commons.sidechain.iroha.util.getAccountDetails

/**
 * Class that is used to get information about D3 clients
 */
class D3ClientProvider(private val notaryQueryAPI: QueryAPI) {

    /**
     * Returns D3 client by its id
     * @param accountId - account id of client
     * @return D3 client
     */
    fun getClient(accountId: String): Result<D3Client, Exception> {
        // Assuming that client sets details himself
        return getAccountDetails(notaryQueryAPI, accountId, accountId)
            .map { accountDetails ->
                D3Client.create(accountId, accountDetails)
            }
    }
}

