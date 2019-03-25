/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.btc.deposit.config

import com.d3.commons.config.BitcoinConfig
import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialConfig

/** Configuration of Bitcoin deposit */
interface BtcDepositConfig {
    /** Web port for health checks */
    val healthCheckPort: Int

    val iroha: IrohaConfig

    val bitcoin: BitcoinConfig

    val notaryCredential: IrohaCredentialConfig

    /** Iroha account to store notary peer list  */
    val notaryListStorageAccount: String

    /** Iroha account to set notary peer list */
    val notaryListSetterAccount: String

    val registrationAccount: String

    val btcTransferWalletPath: String
}
