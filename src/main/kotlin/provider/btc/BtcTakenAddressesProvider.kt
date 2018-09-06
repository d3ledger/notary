package provider.btc

import com.github.kittinunf.result.Result
import config.IrohaConfig
import jp.co.soramitsu.iroha.Keypair
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.getAccountDetails

class BtcTakenAddressesProvider(
    private val irohaConfig: IrohaConfig,
    private val keypair: Keypair,
    private val registrationAccount: String,
    private val notaryAccount: String
) {
    private val irohaNetwork = IrohaNetworkImpl(irohaConfig.hostname, irohaConfig.port)

    fun getTakenAddresses(): Result<Map<String, String>, Exception> {
        return getAccountDetails(
            irohaConfig,
            keypair,
            irohaNetwork,
            notaryAccount,
            registrationAccount
        )
    }
}
