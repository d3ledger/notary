package provider.btc.address

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import model.IrohaCredential
import monitoring.Monitoring
import sidechain.iroha.util.getAccountDetails

//Class that provides all created BTC addresses
class BtcAddressesProvider(
    private val credential: IrohaCredential,
    private val irohaAPI: IrohaAPI,
    private val mstRegistrationAccount: String,
    private val notaryAccount: String
) : Monitoring() {
    override fun monitor() = getAddresses()

    private val queryAPI = QueryAPI(irohaAPI, credential.accountId, credential.keyPair)
    /**
     * Get all created btc addresses
     * @return list full of created BTC addresses
     */
    fun getAddresses(): Result<List<BtcAddress>, Exception> {
        return getAccountDetails(
            queryAPI,
            notaryAccount,
            mstRegistrationAccount
        ).map { addresses ->
            addresses.map { entry ->
                BtcAddress(entry.key, AddressInfo.fromJson(entry.value)!!)
            }
        }
    }
}
