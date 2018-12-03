package provider

import model.IrohaCredential
import mu.KLogging
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.getAccountDetails

/**
 * Provides with list of all notaries peers in the system
 */
class NotaryPeerListProviderImpl(
    private val credential: IrohaCredential,
    private val irohaNetwork: IrohaNetwork,
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
            credential,
            irohaNetwork,
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
