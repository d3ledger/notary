package com.d3.btc.model

import com.squareup.moshi.Moshi
import util.irohaEscape

private val addressInfoJsonAdapter = Moshi.Builder().build().adapter(AddressInfo::class.java)

data class BtcAddress(val address: String, val info: AddressInfo)

/**
 * Data class that holds information about address
 * @param irohaClient - address owner Iroha client id
 * @param notaryKeys - keys that were used to create this address
 * @param nodeId - id of node that created this address
 */
data class AddressInfo(val irohaClient: String?, val notaryKeys: List<String>, val nodeId: String) {

    fun toJson() = addressInfoJsonAdapter.toJson(this).irohaEscape()

    companion object {
        fun fromJson(json: String) = addressInfoJsonAdapter.fromJson(json)
        fun createFreeAddressInfo(notaryKeys: List<String>, nodeId: String) =
            AddressInfo(null, notaryKeys, nodeId)

        fun createChangeAddressInfo(notaryKeys: List<String>, nodeId: String) =
            AddressInfo(null, notaryKeys, nodeId)
    }
}
