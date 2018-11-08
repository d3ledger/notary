package provider.btc.address

import com.squareup.moshi.Moshi

private const val FREE_ACCOUNT = "free"
//Iroha can't stand unescaped quote symbols
private const val IROHA_FRIENDLY_QUOTE = "\\\""

private val addressInfoJsonAdapter = Moshi.Builder().build().adapter(AddressInfo::class.java)

data class BtcAddress(val address: String, val info: AddressInfo)

data class AddressInfo(val irohaClient: String, val notaryKeys: List<String>) {

    fun toJson() = addressInfoJsonAdapter.toJson(this).replace("\"", IROHA_FRIENDLY_QUOTE)

    companion object {
        fun fromJson(json: String) = addressInfoJsonAdapter.fromJson(json.replace(IROHA_FRIENDLY_QUOTE, ""))
        fun createFreeAddressInfo(notaryKeys: List<String>) = AddressInfo(FREE_ACCOUNT, notaryKeys)
    }
}
