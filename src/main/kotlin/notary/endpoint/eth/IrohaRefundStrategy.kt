package notary.endpoint.eth

import notary.endpoint.Refund

/**
 * Strategy for asset refund inside Iroha
 */
interface IrohaRefundStrategy : Refund<IrohaRefundRequest, IrohaNotaryResponse>
