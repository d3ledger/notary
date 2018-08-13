package notary

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import config.IrohaConfig
import jp.co.soramitsu.iroha.Keypair
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.getRelays

/**
 * Implementation of [EthRelayProvider] with Iroha storage.
 *
 * @param irohaConfig - Iroha configuration
 * @param keypair - Iroha keypair to query
 * @param irohaNetwork - iroha network layer
 * @param accountDetailHolder - account that contains details
 * @param accountDetailSetter - account that has set details
 */
class EthRelayProviderIrohaImpl(
    private val irohaConfig: IrohaConfig,
    private val keypair: Keypair,
    private val irohaNetwork: IrohaNetwork,
    private val accountDetailHolder: String,
    private val accountDetailSetter: String
) : EthRelayProvider {

    /**
     * Gets all non free relay wallets
     *
     * @return map<eth_wallet -> iroha_account> in success case or exception otherwise
     */
    override fun getRelays(): Result<Map<String, String>, Exception> {
        return getRelays(
            irohaConfig,
            keypair,
            irohaNetwork,
            accountDetailHolder,
            accountDetailSetter
        ).map { relays ->
            relays.filterValues { it != "free" }
        }
    }
}
