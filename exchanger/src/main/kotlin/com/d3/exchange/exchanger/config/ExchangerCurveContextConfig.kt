/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.exchange.exchanger.config

import com.d3.commons.config.IrohaCredentialRawConfig

interface ExchangerCurveContextConfig {

    /** Iroha exchanger service credential for conversions */
    val irohaCredential: IrohaCredentialRawConfig
}
