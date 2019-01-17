package provider

import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import model.IrohaCredential
import mu.KLogging
import sidechain.iroha.util.getAccountDetails

/**
 * Provides with list of all notaries peers in the system
 */
class NotaryPeerListProviderImpl(
    private val irohaAPI: IrohaAPI,
    private val credential: IrohaCredential,
    private val notaryListStorageAccount: String,
    private val notaryListSetterAccount: String
) : NotaryPeerListProvider {

    private val queryAPI by lazy { QueryAPI(irohaAPI, credential.accountId, credential.keyPair) }

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
