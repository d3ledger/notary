package com.d3.btc.provider

import com.d3.btc.model.AddressInfo
import com.d3.btc.model.BtcAddress
import com.d3.btc.monitoring.Monitoring
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.QueryAPI
import sidechain.iroha.util.getAccountDetails

//Class that provides all registered BTC addresses
open class BtcRegisteredAddressesProvider(
    private val queryAPI: QueryAPI,
    private val registrationAccount: String,
    private val notaryAccount: String
) : Monitoring() {
    override fun monitor() = getRegisteredAddresses()

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
