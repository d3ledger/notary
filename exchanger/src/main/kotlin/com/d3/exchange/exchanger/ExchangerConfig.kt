/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.exchange.exchanger

import com.d3.commons.config.IrohaConfig
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

    /** Liquidity providers account ids */
    val liquidityProviders: String

    /** Asset pairs to be traded details setter account id */
    val tradePairSetter: String

    /** Asset pairs to be traded details key */
    val tradePairKey: String

    /** Asset conversion rate base url **/
    val assetRateBaseUrl: String

    /** Base asset to read rate in comparison with **/
    val baseAssetId: String

    /** Attribute to parse rate from **/
    val rateAttribute: String
}
