package provider.btc.address

import com.squareup.moshi.Moshi
import util.irohaEscape

private const val FREE_ACCOUNT = "free"

private val addressInfoJsonAdapter = Moshi.Builder().build().adapter(AddressInfo::class.java)

data class BtcAddress(val address: String, val info: AddressInfo)

data class AddressInfo(val irohaClient: String, val notaryKeys: List<String>) {

    fun toJson() = String.irohaEscape(addressInfoJsonAdapter.toJson(this))

    companion object {
        fun fromJson(json: String) = addressInfoJsonAdapter.fromJson(json)
        fun createFreeAddressInfo(notaryKeys: List<String>) = AddressInfo(FREE_ACCOUNT, notaryKeys)
    }
}
