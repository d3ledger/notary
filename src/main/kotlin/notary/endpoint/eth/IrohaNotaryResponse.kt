package notary.endpoint.eth

/**
 * Interface represents notary response with proof of rollback from Iroha to Ethereum
 */
sealed class IrohaNotaryResponse {
    /**
     * Successful response that contains proof
     */
    data class Successful(

        /** Signature of [Successful] via Iroha private key */
        val irohaResultingTx: String

    ) : IrohaNotaryResponse()

    /**
     * Error response which contains reason of error
     */
    data class Error(

        /** Human-readable explanation of error */
        val reason: String

    ) : IrohaNotaryResponse()
}

