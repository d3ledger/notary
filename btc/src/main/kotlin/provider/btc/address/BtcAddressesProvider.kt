package provider.btc.address

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
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
     * @return list full of created BTC addresses
     */
    fun getAddresses(): Result<List<BtcAddress>, Exception> {
        return getAccountDetails(
            credential,
            irohaNetwork,
            notaryAccount,
            mstRegistrationAccount
        ).map { addresses ->
            addresses.map { entry ->
                BtcAddress(entry.key, AddressInfo.fromJson(entry.value)!!)
            }
        }
    }
}
