package com.d3.eth.commons


/**
 * Type of transaction hash in Iroha
 */
typealias IrohaTransactionHashType = String

/**
 * Interface represents custodian's request about rollback from Iroha to Ethereum
 * @param irohaTx - Hash of Iroha transaction for requesting refund
 */
data class EthRefundRequest(
    val irohaTx: IrohaTransactionHashType
)

