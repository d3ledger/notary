/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.token

/** Information about token - token [name], [domain] and [precision] */
data class EthTokenInfo(val name: String, val domain: String, val precision: Int)
