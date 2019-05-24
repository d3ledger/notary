/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.exchange.exchanger

import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialConfig
import com.d3.commons.config.IrohaCredentialRawConfig

/**
 * Interface represents configs for exchanger service
 */
interface ExchangerConfig {

    /** Iroha configs */
    val iroha: IrohaConfig

    /** Iroha registration service credential */
    val irohaCredential: IrohaCredentialRawConfig

    /** RMQ queue name */
    val irohaBlockQueue: String

    /** Liquidity providers account names */
    val liquidityProviders: String
}
