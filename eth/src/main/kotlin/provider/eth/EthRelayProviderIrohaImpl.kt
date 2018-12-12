package provider.eth

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.IrohaAPI
import model.IrohaCredential
import mu.KLogging
import sidechain.iroha.util.getAccountDetails

/**
 * Implementation of [EthRelayProvider] with Iroha storage.
 *
 * @param irohaConfig - Iroha configuration
 * @param keypair - Iroha keypair to query
 * @param notaryAccount - account that contains details
 * @param registrationAccount - account that has set details
 */
class EthRelayProviderIrohaImpl(
    private val irohaAPI: IrohaAPI,
    private val credential: IrohaCredential,
    private val notaryAccount: String,
    private val registrationAccount: String
) : EthRelayProvider {

    init {
        logger.info {
            "Init relay provider with notary account '$notaryAccount' and registration account '$registrationAccount'"
        }
    }

    /**
     * Gets all non free relay wallets
     *
     * @return map<eth_wallet -> iroha_account> in success case or exception otherwise
     */
    override fun getRelays(): Result<Map<String, String>, Exception> {
        return getAccountDetails(
            irohaAPI,
            credential,
            notaryAccount,
            registrationAccount
        ).map { relays ->
            relays.filterValues { it != "free" }
        }
    }

    /** Get relay belonging to [irohaAccountId] */
    override fun getRelay(irohaAccountId: String): Result<String, Exception> {
        return getRelays().map { relays ->
            relays.filter { it.value == irohaAccountId }.keys.first()
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
