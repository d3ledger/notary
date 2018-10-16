package provider.btc

import com.github.kittinunf.result.Result
import model.IrohaCredential
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.getAccountDetails

//Class that provides all registered BTC addresses
class BtcRegisteredAddressesProvider(
    private val credential: IrohaCredential,
    private val irohaNetwork: IrohaNetwork,
    private val registrationAccount: String,
    private val notaryAccount: String
) {
    /**
     * Get all registered btc addresses
     * @return map full of registered btc addresses (btc address -> iroha account name)
     */
    fun getRegisteredAddresses(): Result<Map<String, String>, Exception> {
        return getAccountDetails(
            credential,
            irohaNetwork,
            notaryAccount,
            registrationAccount
        )
    }
}
