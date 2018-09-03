package provider

import jp.co.soramitsu.iroha.Keypair
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.getAccountDetails
import withdrawalservice.WithdrawalServiceConfig

/**
 * Provides with list of all notaries peers in the system
 */
class NotaryPeerListProviderImpl : NotaryPeerListProvider {

    override fun getPeerList(
        withdrawalServiceConfig: WithdrawalServiceConfig,
        keypair: Keypair,
        irohaNetwork: IrohaNetwork
    ): List<PeerAddress> {
        val notaries = getAccountDetails(
            withdrawalServiceConfig.iroha,
            keypair,
            irohaNetwork,
            withdrawalServiceConfig.notaryStorageAccount,
            withdrawalServiceConfig.detailSetterAccount
        )

        return notaries.get().values.toList()
    }
}
