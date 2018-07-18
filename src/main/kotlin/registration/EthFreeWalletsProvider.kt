package registration

import config.IrohaConfig
import jp.co.soramitsu.iroha.Keypair
import mu.KLogging
import sidechain.iroha.util.getRelays

/**
 * Provides with free ethereum relay wallet
 * @param keypair - iroha keypair
 * @param notaryIrohaAccount - Master notary account in Iroha to write down the information about free relay wallets has been added
 */
// TODO Prevent double relay accounts usage (in perfect world it is on Iroha side with custom code). In real world
// on provider side with some synchronization.
class EthFreeWalletsProvider(
    val irohaConfig: IrohaConfig,
    val keypair: Keypair,
    val notaryIrohaAccount: String,
    val registrationIrohaAccount: String
) {
    fun getWallet(): String {
        return getRelays(irohaConfig, notaryIrohaAccount, registrationIrohaAccount).filterValues { it == "free" }
            .keys.first()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
