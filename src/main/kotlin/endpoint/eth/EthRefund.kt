package endpoint.eth

import java.math.BigInteger

/**
 * Type of address in Ethereum
 */
typealias Address = String

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
data class EthRefund(val address: Address, val type: CoinType, val amount: AmountType)
