package com.d3.btc.provider

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
    override fun monitor() = getAllChangeAddresses()

    /**
     * Returns all change addresses
     */
    fun getAllChangeAddresses(): Result<List<BtcAddress>, Exception> {
        return getAccountDetails(
            queryAPI,
            changeAddressesStorageAccount,
            mstRegistrationAccount
        ).map { details ->
            // Map details into BtcAddress collection
            details.entries.map { entry ->
                BtcAddress(entry.key, AddressInfo.fromJson(entry.value)!!)
            }
        }
    }


    /**
     * Returns all change addresses that were generated before given time
     * @param generatedBefore - only addresses that were generated before this value will be returned
     */
    fun getAllChangeAddresses(generatedBefore: Long): Result<List<BtcAddress>, Exception> {
        return getAllChangeAddresses().map { changeAddresses ->
            changeAddresses
                .filter { changeAddress -> changeAddress.info.generationTime!! <= generatedBefore }
        }
    }
}
