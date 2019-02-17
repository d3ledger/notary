package withdrawal.btc.provider

import com.d3.btc.model.AddressInfo
import com.d3.btc.model.BtcAddress
import com.d3.btc.monitoring.Monitoring
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.QueryAPI
import sidechain.iroha.util.getAccountDetails

/*
    Class that is used to get change address
 */
open class BtcChangeAddressProvider(
    private val queryAPI: QueryAPI,
    private val mstRegistrationAccount: String,
    private val changeAddressesStorageAccount: String
) : Monitoring() {
    override fun monitor() = getChangeAddress()

    /**
     * Returns change address
     * @return - result with change address object
     */
    fun getChangeAddress(): Result<BtcAddress, Exception> {
        return getAccountDetails(
            queryAPI,
            changeAddressesStorageAccount,
            mstRegistrationAccount
        ).map { addresses ->
            if (addresses.isEmpty()) {
                throw IllegalStateException("No change address was set")
            }
            val changeAddressEntry = addresses.entries.first()
            BtcAddress(changeAddressEntry.key, AddressInfo.fromJson(changeAddressEntry.value)!!)
        }
    }
}
