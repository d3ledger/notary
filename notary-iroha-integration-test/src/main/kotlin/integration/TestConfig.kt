/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration

import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialRawConfig

interface TestConfig {
    val iroha: IrohaConfig
    val testCredentialConfig: IrohaCredentialRawConfig
    val testQueue: String
    val clientStorageAccount: String
    val brvsAccount: String
}
