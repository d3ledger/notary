/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.bootstrap.dto

import com.d3.commons.config.EthereumConfig
import com.d3.commons.config.EthereumPasswords
import org.web3j.crypto.WalletFile

data class EthereumNetworkProperties(
    val ethPasswords: EthereumPasswordsImpl = EthereumPasswordsImpl(),
    val ethereumConfig: EthereumConfigImpl = EthereumConfigImpl()
)

data class EthereumPasswordsImpl(
    override val credentialsPassword: String = "user",
    override val nodeLogin: String? = null,
    override val nodePassword: String? = null
) : EthereumPasswords

/**
 * Default parameters are Ropsten testnet parameters
 */
data class EthereumConfigImpl(
    override val url: String = "http://parity-d3.test.iroha.tech:8545",
    override val credentialsPath: String = "some\\path\\to\\genesis.key",
    override val gasPrice: Long = 100000000000,
    override val gasLimit: Long = 4500000,
    override val confirmationPeriod: Long = 0
) : EthereumConfig

data class EthWallet(val file: WalletFile? = null) : Conflictable()
