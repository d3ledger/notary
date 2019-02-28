package com.d3.commons.notary.endpoint.eth

import com.d3.commons.notary.endpoint.Refund

/**
 * Strategy for ethereum refund
 */
interface EthRefundStrategy : Refund<EthRefundRequest, EthNotaryResponse>
