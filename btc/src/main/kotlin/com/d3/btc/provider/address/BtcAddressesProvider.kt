package com.d3.btc.provider.address

import com.d3.btc.model.AddressInfo
import com.d3.btc.model.BtcAddress
import com.d3.btc.monitoring.Monitoring
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.QueryAPI
import com.d3.commons.sidechain.iroha.util.getAccountDetails

//Class that provides all created BTC addresses
class BtcAddressesProvider(
    private val queryAPI: QueryAPI,
    private val mstRegistrationAccount: String,
    private val notaryAccount: String
) : Monitoring() {
    override fun monitor() = getAddresses()

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
