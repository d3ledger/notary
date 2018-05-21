package endpoint.eth

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson


/**
 * Interface represents notary response with proof of rollback from Iroha to Ethereum
 */
sealed class EthNotaryResponse {
    /**
     * Successful response that contains proof
     */
    data class Successful(
        val ethSignature: EthSignature,
        val ethPublicKey: EthPublicKey
    ) : EthNotaryResponse()

    /**
     * Error response which contains reason of error
     */
    data class Error(
        val code: Int,
        val reason: String
    ) : EthNotaryResponse()
}

// Code below is boilerplate which is required for supporting sealed classes in Moshi

/**
 * Enum type with [EthNotaryResponse]'s inheritors classes
 */
enum class EthNotaryResponseType {
    Successful, Error
}

/**
 * Union data class which contains all fields of all inheritors of [EthNotaryResponse].
 * Class is required for trivial transformation of [EthNotaryResponse] to JSON and vice versa
 */
data class EthNotaryResponseLayer(
    val type: EthNotaryResponseType,
    val ethSignature: EthSignature? = null,
    val ethPublicKey: EthPublicKey? = null,
    val code: Int? = null,
    val reason: String? = null
)

/**
 * JSON adapter for Moshi builder
 */
class EthNotaryResponseMoshiAdapter {

    /**
     * Conversion from [EthNotaryResponseLayer] which is JSON representation to [EthNotaryResponse] subtype
     * @param layer instance of JSON object
     */
    @FromJson
    fun fromJson(layer: EthNotaryResponseLayer) = when (layer.type) {
        EthNotaryResponseType.Successful -> EthNotaryResponse.Successful(layer.ethSignature!!, layer.ethPublicKey!!)
        EthNotaryResponseType.Error -> EthNotaryResponse.Error(layer.code!!, layer.reason!!)
    }

    /**
     *  Conversion from [EthNotaryResponse] to [EthNotaryResponseLayer]
     *  @param response business object for transformation to JSON
     */
    @ToJson
    fun toJson(response: EthNotaryResponse) = when (response) {
        is EthNotaryResponse.Successful -> EthNotaryResponseLayer(
            type = EthNotaryResponseType.Successful,
            ethSignature = response.ethSignature,
            ethPublicKey = response.ethPublicKey
        )
        is EthNotaryResponse.Error -> EthNotaryResponseLayer(
            type = EthNotaryResponseType.Successful,
            code = response.code,
            reason = response.reason
        )
    }
}

