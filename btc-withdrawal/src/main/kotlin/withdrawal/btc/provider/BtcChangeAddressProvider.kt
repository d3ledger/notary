package withdrawal.btc.provider

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import model.IrohaCredential
import provider.btc.address.AddressInfo
import provider.btc.address.BtcAddress
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.getAccountDetails

/*
    Class that is used to get change address
 */

open class BtcChangeAddressProvider(
    private val credential: IrohaCredential,
    private val irohaNetwork: IrohaNetwork,
    private val mstRegistrationAccount: String,
    private val changeAddressesStorageAccount: String
) {
    /**
     * Returns change address
     * @return - result with change address object
     */
    fun getChangeAddress(): Result<BtcAddress, Exception> {
        return getAccountDetails(
            credential,
            irohaNetwork,
            changeAddressesStorageAccount,
            mstRegistrationAccount
        ).map { addresses ->
            val changeAddressEntry = addresses.entries.first()
            BtcAddress(changeAddressEntry.key, AddressInfo.fromJson(changeAddressEntry.value)!!)
        }
    }
}
