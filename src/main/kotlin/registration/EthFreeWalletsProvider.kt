package registration

import config.ConfigKeys
import jp.co.soramitsu.iroha.Keypair
import mu.KLogging
import notary.CONFIG
import util.iroha.getRelays

/**
 * Provides with free ethereum relay wallet
 * @param keypair - iroha keypair
 * @param notaryIrohaAccount - Master notary account in Iroha to write down the information about free relay wallets has been added
 */
// TODO Prevent double relay accounts usage (in perfect world it is on Iroha side with custom code). In real world
// on provider side with some synchronization.
class EthFreeWalletsProvider(
    val keypair: Keypair,
    val notaryIrohaAccount: String = CONFIG[ConfigKeys.registrationServiceNotaryIrohaAccount]
) {
    fun getWallet(): String {
        return getRelays(notaryIrohaAccount).filterValues { it == "free" }.keys.first()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
