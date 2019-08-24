/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.config

interface TestConfig {
    val ethTestAccount: String
    val iroha: IrohaConfig
    val testCredentialConfig: IrohaCredentialRawConfig
}
