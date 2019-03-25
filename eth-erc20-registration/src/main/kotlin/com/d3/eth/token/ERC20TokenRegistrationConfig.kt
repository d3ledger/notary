/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.token

import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialConfig

interface ERC20TokenRegistrationConfig {

    /** Iroha credentials */
    val irohaCredential: IrohaCredentialConfig

    /** Iroha config */
    val iroha: IrohaConfig

    /** Path to file full of tokens in json format */
    val tokensFilePath: String

    /** Account that stores tokens */
    val tokenStorageAccount: String

    /** Address of XOR ERC20 contract */
    val xorEthereumAddress: String
}
