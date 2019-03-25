/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.btc.provider.network

import org.bitcoinj.params.MainNetParams
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("mainnet")
@Component
class BtcMainNetConfigProvider : BtcNetworkConfigProvider {
    override fun getConfig() = MainNetParams.get()
}
