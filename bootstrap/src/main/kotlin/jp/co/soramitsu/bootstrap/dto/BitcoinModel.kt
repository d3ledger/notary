/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.bootstrap.dto

import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.params.TestNet3Params

data class BtcWallet(val file: String? = null, val network: BtcNetwork? = null) : Conflictable()

enum class BtcNetwork(val params: NetworkParameters) {
    RegTest(RegTestParams.get()),
    TestNet3(TestNet3Params.get()),
    MainNet(MainNetParams.get())
}
