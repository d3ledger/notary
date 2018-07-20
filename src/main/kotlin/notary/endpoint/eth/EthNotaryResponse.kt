package notary.endpoint.eth

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

        /** Signature of [Successful.ethRefund] via Ethereum private key */
        val ethSignature: EthSignature,

        /** Refund object for Ethereum relay smart-contract */
        val ethRefund: EthRefund

    ) : EthNotaryResponse()

    /**
     * Error response which contains reason of error
     */
    data class Error(

        /** Human-readable explanation of error */
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
    val ethRefund: EthRefund? = null,
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
        EthNotaryResponseType.Successful -> EthNotaryResponse.Successful(layer.ethSignature!!, layer.ethRefund!!)
        EthNotaryResponseType.Error -> EthNotaryResponse.Error(layer.reason!!)
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
            ethRefund = response.ethRefund
        )
        is EthNotaryResponse.Error -> EthNotaryResponseLayer(
            type = EthNotaryResponseType.Error,
            reason = response.reason
        )
    }
}

