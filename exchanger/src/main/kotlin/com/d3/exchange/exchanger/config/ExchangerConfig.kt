/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.exchange.exchanger.config

import com.d3.commons.config.IrohaConfig

/**
 * Interface represents configs for exchanger service
 */
interface ExchangerConfig {

    /** Iroha configs */
    val iroha: IrohaConfig

    /** RMQ queue name */
    val irohaBlockQueue: String

    /** Liquidity providers account ids */
    val liquidityProviders: String

    /** Asset pairs to be traded details setter account id */
    val tradePairSetter: String

    /** Asset pairs to be traded details key */
    val tradePairKey: String

    /** Fee fraction 0.(0)1..1 1 means no fee**/
    val feeFraction: String
}
