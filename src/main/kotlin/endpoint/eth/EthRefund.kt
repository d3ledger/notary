package endpoint.eth

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.math.BigInteger

/**
 * Type of address in Ethereum
 */
typealias EthereumAddress = String

/**
 * Type of coin in Ethereum. Possibly it will be an address
 */
typealias CoinType = String

/**
 * Type of amount in Ethereum
 */
typealias AmountType = BigInteger

/**
 * Refund structure
 */
data class EthRefund(
    val address: EthereumAddress,
    val assetId: CoinType,
    val amount: AmountType,
    val irohaTxHash: IrohaTransactionHashType
)

/**
 * Adapter of [BigInteger] class for moshi
 */
class BigIntegerMoshiAdapter : JsonAdapter<BigInteger>() {

    override fun fromJson(reader: JsonReader?): BigInteger? {
        return BigInteger(reader?.nextString(), 10)
    }

    override fun toJson(writer: JsonWriter?, value: BigInteger?) {
        writer?.value(value?.toString(10))
    }
}
