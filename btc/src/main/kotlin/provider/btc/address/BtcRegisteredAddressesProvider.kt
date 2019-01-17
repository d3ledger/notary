package provider.btc.address

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import model.IrohaCredential
import monitoring.Monitoring
import sidechain.iroha.util.getAccountDetails

//Class that provides all registered BTC addresses
open class BtcRegisteredAddressesProvider(
    private val credential: IrohaCredential,
    private val irohaAPI: IrohaAPI,
    private val registrationAccount: String,
    private val notaryAccount: String
) : Monitoring() {
    override fun monitor() = getRegisteredAddresses()

    private val queryAPI by lazy { QueryAPI(irohaAPI, credential.accountId, credential.keyPair) }

    /**
     * Get all registered btc addresses
     * @return list full of registered BTC addresses
     */
    fun getRegisteredAddresses(): Result<List<BtcAddress>, Exception> {
        return getAccountDetails(
            queryAPI,
            notaryAccount,
            registrationAccount
        ).map { addresses ->
            addresses.map { entry ->
                BtcAddress(entry.key, AddressInfo.fromJson(entry.value)!!)
            }
        }
    }
}
