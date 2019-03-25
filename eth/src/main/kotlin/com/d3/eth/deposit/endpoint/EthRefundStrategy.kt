/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.deposit.endpoint

import com.d3.commons.notary.endpoint.Refund

/**
 * Strategy for ethereum refund
 */
interface EthRefundStrategy :
    Refund<EthRefundRequest, EthNotaryResponse>
