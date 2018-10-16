package provider.btc

import com.github.kittinunf.result.Result
import model.IrohaCredential
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.getAccountDetails

//Class that provides all created BTC addresses
class BtcAddressesProvider(
    private val credential: IrohaCredential,
    private val irohaNetwork: IrohaNetwork,
    private val mstRegistrationAccount: String,
    private val notaryAccount: String
) {
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
