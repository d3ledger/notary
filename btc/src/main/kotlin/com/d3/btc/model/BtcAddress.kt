package com.d3.btc.model

import com.squareup.moshi.Moshi
import util.irohaEscape

private val addressInfoJsonAdapter = Moshi.Builder().build().adapter(AddressInfo::class.java)

data class BtcAddress(val address: String, val info: AddressInfo) {
    fun isFree() = this.info.irohaClient == BtcAddressType.FREE.title
    fun isChange() = this.info.irohaClient == BtcAddressType.CHANGE.title
}

data class AddressInfo(val irohaClient: String, val notaryKeys: List<String>) {

    fun toJson() = addressInfoJsonAdapter.toJson(this).irohaEscape()

    companion object {
        fun fromJson(json: String) = addressInfoJsonAdapter.fromJson(json)
        fun createFreeAddressInfo(notaryKeys: List<String>) = AddressInfo(BtcAddressType.FREE.title, notaryKeys)
        fun createChangeAddressInfo(notaryKeys: List<String>) = AddressInfo(BtcAddressType.CHANGE.title, notaryKeys)
    }
}
