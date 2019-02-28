package com.d3.commons.provider

import jp.co.soramitsu.iroha.java.QueryAPI
import mu.KLogging
import com.d3.commons.sidechain.iroha.util.getAccountDetails

/**
 * Provides with list of all notaries peers in the system
 */
class NotaryPeerListProviderImpl(
    private val queryAPI: QueryAPI,
    private val notaryListStorageAccount: String,
    private val notaryListSetterAccount: String
) : NotaryPeerListProvider {

    init {
        logger.info {
            "Init notary peer list provider with notary list storage account '$notaryListStorageAccount'" +
                    " and notary list setter account '$notaryListSetterAccount'"
        }
    }

    override fun getPeerList(
    ): List<PeerAddress> {
        return getAccountDetails(
            queryAPI,
            notaryListStorageAccount,
            notaryListSetterAccount
        ).fold(
            { notaries -> notaries.values.toList() },
            { ex -> throw ex })
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
