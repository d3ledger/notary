package notary.endpoint.eth

import notary.endpoint.Refund

/**
 * Strategy for ethereum refund
 */
interface EthRefundStrategy : Refund<EthRefundRequest, EthNotaryResponse>
