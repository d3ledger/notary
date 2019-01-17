package withdrawal.btc.provider

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import model.IrohaCredential
import monitoring.Monitoring
import provider.btc.address.AddressInfo
import provider.btc.address.BtcAddress
import sidechain.iroha.util.getAccountDetails

/*
    Class that is used to get change address
 */
open class BtcChangeAddressProvider(
    private val credential: IrohaCredential,
    private val irohaAPI: IrohaAPI,
    private val mstRegistrationAccount: String,
    private val changeAddressesStorageAccount: String
) : Monitoring() {
    override fun monitor() = getChangeAddress()

    private val queryAPI by lazy { QueryAPI(irohaAPI, credential.accountId, credential.keyPair) }

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
            val changeAddressEntry = addresses.entries.first()
            BtcAddress(changeAddressEntry.key, AddressInfo.fromJson(changeAddressEntry.value)!!)
        }
    }
}
