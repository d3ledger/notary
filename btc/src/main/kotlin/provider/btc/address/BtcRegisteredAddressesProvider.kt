package provider.btc.address

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import model.IrohaCredential
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.getAccountDetails

//Class that provides all registered BTC addresses
open class BtcRegisteredAddressesProvider(
    private val credential: IrohaCredential,
    private val irohaNetwork: IrohaNetwork,
    private val registrationAccount: String,
    private val notaryAccount: String
) {

    /**
     * Get all registered btc addresses
     * @return list full of registered BTC addresses
     */
    fun getRegisteredAddresses(): Result<MutableList<BtcAddress>, Exception> {
        return getAccountDetails(
            credential,
            irohaNetwork,
            notaryAccount,
            registrationAccount
        ).map { addresses ->
            addresses.map { entry ->
                BtcAddress(entry.key, AddressInfo.fromJson(entry.value)!!)
            }.toMutableList()
        }
    }
}
