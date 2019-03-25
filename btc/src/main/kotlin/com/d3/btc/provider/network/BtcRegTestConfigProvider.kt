/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.btc.provider.network

import org.bitcoinj.params.RegTestParams
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("local")
@Component
class BtcRegTestConfigProvider : BtcNetworkConfigProvider {
    override fun getConfig() = RegTestParams.get()
}
