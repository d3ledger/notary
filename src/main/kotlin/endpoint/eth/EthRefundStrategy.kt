package endpoint.eth

import endpoint.Refund

/**
 * Strategy for ethereum refund
 */
interface EthRefundStrategy : Refund<EthRefundContract, EthNotaryResponse>
