package notary.endpoint.eth

import notary.endpoint.Refund

/**
 * Strategy for asset refund inside Iroha
 */
// So far, so simple
interface IrohaRefundStrategy : Refund<IrohaRefundRequest, IrohaNotaryResponse>
