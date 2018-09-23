package provider

import config.IrohaConfig
import jp.co.soramitsu.iroha.Keypair
import model.IrohaCredential
import mu.KLogging
import provider.eth.EthRelayProviderIrohaImpl
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.getAccountDetails

/**
 * Provides with list of all notaries peers in the system
 */
class NotaryPeerListProviderImpl(
    private val iroha: IrohaConfig,
    private val credential: IrohaCredential,
    private val notaryListStorageAccount: String,
    private val notaryListSetterAccount: String
) : NotaryPeerListProvider {

    init {
        EthRelayProviderIrohaImpl.logger.info {
            "Init notary peer list provider with notary list storage account '$notaryListStorageAccount'" +
                    " and notary list setter account '$notaryListSetterAccount'"
        }
    }

    private val irohaNetwork = IrohaNetworkImpl(iroha.hostname, iroha.port)

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
