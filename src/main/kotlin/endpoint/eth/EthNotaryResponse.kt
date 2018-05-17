package endpoint.eth


/**
 * Interface represents notary response with proof of rollback from Iroha to Ethereum
 */
sealed class EthNotaryResponse

/**
 * Successful response of
 */
interface SuccesfullResponse {
    /**
     * getter for eth signature
     */
    fun ethSignature(): EthSignature

    /**
     * getter for input contract
     */
    fun ethContract(): EthRefundContract
}

interface ErrorResponse {

    /**
     * getter of code error
     */
    fun code(): Int

    /**
     * getter of user representation of the problem
     */
    fun reason(): String
}
