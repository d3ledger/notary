package com.d3.btc.withdrawal.provider

import com.d3.btc.model.AddressInfo
import com.d3.btc.model.BtcAddress
import com.d3.btc.monitoring.Monitoring
import com.d3.commons.sidechain.iroha.util.getAccountDetails
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.QueryAPI

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
     * Returns all change addresses
     */
    fun getAllChangeAddresses(): Result<List<BtcAddress>, Exception> {
        return getAccountDetails(
            queryAPI,
            changeAddressesStorageAccount,
            mstRegistrationAccount
        ).map { details ->
            if (details.isEmpty()) {
                throw IllegalStateException("No change address was set")
            }
            // Map details into BtcAddress collection
            details.entries.map { entry ->
                BtcAddress(entry.key, AddressInfo.fromJson(entry.value)!!)
            }
        }
    }

    /**
     * Returns current change address
     * @return - result with change address object
     */
    fun getChangeAddress(): Result<BtcAddress, Exception> {
        return getAllChangeAddresses().map { changeAddresses ->
            // Get the newest change address
            changeAddresses.maxBy { address -> address.info.generationTime ?: 0 }!!
        }
    }
}
