/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.deposit

import com.d3.commons.config.EthereumConfig
import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialConfig

/** Configuration of refund endpoint in Notary */
interface RefundConfig {
    val port: Int
}

/** Configuration of deposit */
interface EthDepositConfig {
    /** Iroha account that has registered wallets */
    val registrationServiceIrohaAccount: String

    /** Iroha account that stores tokens */
    val tokenStorageAccount: String

    /** Iroha account sets tokens */
    val tokenSetterAccount: String

    /** Iroha account that set whitelist for client */
    val whitelistSetter: String

    /** Iroha account to store notary peer list  */
    val notaryListStorageAccount: String

    /** Iroha account to set notary peer list */
    val notaryListSetterAccount: String

    val notaryCredential: IrohaCredentialConfig

    val refund: RefundConfig

    val iroha: IrohaConfig

    val ethereum: EthereumConfig

    /** Iroha withdrawal account grant permission to */
    val withdrawalAccountId: String
}
