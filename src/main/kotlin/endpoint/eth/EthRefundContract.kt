package endpoint.eth

/**
 * Interface represents custodian request about rollback from Iroha to Ethereum
 */
interface EthRefundContract {
    fun getEthAddress(): EthAddress
    fun getEthTokenType(): EthTokenType
    fun getEthAsset(): EthTokenAsset
}

/**
 * Number of asset for transfer
 */
interface EthTokenAsset

/**
 * Eth token identifier
 */
interface EthTokenType

/**
 * Target address for the transfer in Eth network
 */
interface EthAddress
