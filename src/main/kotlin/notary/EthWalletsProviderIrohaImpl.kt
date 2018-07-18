package notary

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import config.IrohaConfig
import jp.co.soramitsu.iroha.Keypair
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.getRelays

/**
 * Implementation of [EthWalletsProvider] with Iroha storage.
 *
 * @param irohaConfig - Iroha configuration
 * @param keypair - Iroha keypair to query
 * @param relayRegistrationAccount - account of a registration service that has set details
 * @param registrationServiceNotaryIrohaAccount - notary account that contains details
 */
class EthWalletsProviderIrohaImpl(
    val irohaConfig: IrohaConfig,
    val keypair: Keypair,
    val irohaNetwork: IrohaNetwork,
    val relayRegistrationAccount: String,
    val registrationServiceNotaryIrohaAccount: String
) : EthWalletsProvider {

    /**
     * Account of registration service that has set details.
     *
     * @return map<eth_wallet -> iroha_account> in success case or exception otherwise
     */
    override fun getWallets(): Result<Map<String, String>, Exception> {
        return getRelays(
            irohaConfig,
            keypair,
            irohaNetwork,
            relayRegistrationAccount,
            registrationServiceNotaryIrohaAccount
        ).map { it.filterValues { it != "free" } }
    }
}
