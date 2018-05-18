package endpoint.eth

/**
 * Interface represents custodian request about rollback from Iroha to Ethereum
 */
data class EthRefundContract(
    val ethAddress: EthAddress,
    val ethTokenType: EthTokenType,
    val ethAmountAsset: EthAmountAsset
)

/**
 * Number of asset for transfer
 */
typealias EthAmountAsset = Float

/**
 * Eth token identifier
 */
typealias EthTokenType = String

/**
 * Target address for the transfer in Eth network
 */
typealias EthAddress = String
