package registration

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import config.IrohaConfig
import jp.co.soramitsu.iroha.Keypair
import mu.KLogging
import sidechain.iroha.consumer.IrohaNetworkImpl
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
    val irohaNetwork = IrohaNetworkImpl(irohaConfig.hostname, irohaConfig.port)

    fun getWallet(): Result<String, Exception> {
        return getRelays(irohaConfig, keypair, irohaNetwork, notaryIrohaAccount, registrationIrohaAccount)
            .map {
                val freeWallets = it.filterValues { it == "free" }.keys
                if (freeWallets.isEmpty())
                    throw Exception("EthFreeWalletsProvider - no free relay wallets")
                else
                    freeWallets.first()
            }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
