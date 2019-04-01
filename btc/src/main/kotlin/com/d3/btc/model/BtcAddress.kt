package com.d3.btc.model

import com.squareup.moshi.Moshi
import com.d3.commons.util.irohaEscape

private val addressInfoJsonAdapter = Moshi.Builder().build().adapter(AddressInfo::class.java)

data class BtcAddress(val address: String, val info: AddressInfo)

/**
 * Data class that holds information about address
 * @param irohaClient - address owner Iroha client id
 * @param notaryKeys - keys that were used to create this address
 * @param nodeId - id of node that created this address
 * @param generationTime - time of address generation
 */
data class AddressInfo(
    val irohaClient: String?,
    val notaryKeys: List<String>,
    val nodeId: String,
    val generationTime: Long?
) {

    fun toJson() = addressInfoJsonAdapter.toJson(this).irohaEscape()

    companion object {
        fun fromJson(json: String) = addressInfoJsonAdapter.fromJson(json)
        fun createFreeAddressInfo(notaryKeys: List<String>, nodeId: String, generationTime: Long) =
            AddressInfo(null, notaryKeys, nodeId, generationTime)

        fun createChangeAddressInfo(notaryKeys: List<String>, nodeId: String, generationTime: Long) =
            AddressInfo(null, notaryKeys, nodeId, generationTime)
    }
}
