package endpoint.eth


/**
 * Type of transaction hash in Iroha
 */
typealias IrohaTransactionHashType = String

/**
 * Interface represents custodian's request about rollback from Iroha to Ethereum
 */
data class EthRefundRequest(
    /** Hash of Iroha transaction for requesting refund */
    val irohaTx: IrohaTransactionHashType
)

