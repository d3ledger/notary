/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.exchange.exchanger.config

import com.d3.commons.config.IrohaCredentialRawConfig

interface ExchangerDcConfig {

    /** Iroha exchanger service credential for conversions */
    val irohaCredential: IrohaCredentialRawConfig

    /** Asset conversion rate base url **/
    val assetRateBaseUrl: String

    /** Base asset to read rate in comparison with **/
    val baseAssetId: String

    /** Attribute to parse rate from **/
    val rateAttribute: String
}
