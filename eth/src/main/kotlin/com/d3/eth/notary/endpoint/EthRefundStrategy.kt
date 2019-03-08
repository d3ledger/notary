package com.d3.eth.notary.endpoint

import com.d3.commons.notary.endpoint.Refund
import com.d3.eth.notary.endpoint.EthNotaryResponse
import com.d3.eth.notary.endpoint.EthRefundRequest

/**
 * Strategy for ethereum refund
 */
interface EthRefundStrategy :
    Refund<EthRefundRequest, EthNotaryResponse>
