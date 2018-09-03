package provider

import jp.co.soramitsu.iroha.Keypair
import sidechain.iroha.consumer.IrohaNetwork

typealias PeerAddress = String

/**
 * Provides with list of all notaries
 */
interface NotaryPeerListProvider {
    fun getPeerList(
        withdrawalServiceConfig: WithdrawalServiceConfig,
        keypair: Keypair,
        irohaNetwork: IrohaNetwork
    ): List<PeerAddress>
}
