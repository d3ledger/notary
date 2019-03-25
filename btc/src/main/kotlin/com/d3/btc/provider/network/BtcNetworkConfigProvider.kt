/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.btc.provider.network

import org.bitcoinj.core.NetworkParameters

interface BtcNetworkConfigProvider {
    fun getConfig(): NetworkParameters
}
