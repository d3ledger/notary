package endpoint.eth


/**
 * Type of transaction hash in Iroha
 */
typealias IrohaTransactionHashType = String

/**
 * Interface represents custodian's request about rollback from Iroha to Ethereum
 * @param destEthAddress - ethereum address to transfer assets
 * @param irohaTx - Hash of Iroha transaction for requesting refund
 */
data class EthRefundRequest(
    val destEthAddress: EthereumAddress,
    val irohaTx: IrohaTransactionHashType
)

