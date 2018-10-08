package provider.btc

import com.github.kittinunf.result.Result
import config.IrohaConfig
import jp.co.soramitsu.iroha.Keypair
import model.IrohaCredential
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.getAccountDetails

//Class that provides all created BTC addresses
class BtcAddressesProvider(
    irohaConfig: IrohaConfig,
    private val credential: IrohaCredential,
    private val mstRegistrationAccount: String,
    private val notaryAccount: String
) {
    private val irohaNetwork = IrohaNetworkImpl(irohaConfig.hostname, irohaConfig.port)
    /**
     * Get all created btc addresses
     * @return map full of created btc addresses (btc address -> iroha account name)
     */
    fun getAddresses(): Result<Map<String, String>, Exception> {
        return getAccountDetails(
            credential,
            irohaNetwork,
            notaryAccount,
            mstRegistrationAccount
        )
    }
}
