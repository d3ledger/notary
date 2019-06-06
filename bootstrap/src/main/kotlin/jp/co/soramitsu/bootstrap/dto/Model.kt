/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.bootstrap.dto

import javax.validation.constraints.NotNull

interface DtoFactory<out T> {
    fun getDTO(): T
}

open class Conflictable(var errorCode: String? = null, var message: String? = null)

data class BlockchainCreds(
    val private: String? = null,
    val public: String? = null,
    val address: String? = null,
    val confirmationPeriod: Int? = null
) : Conflictable()

data class Peer(
    @NotNull val peerKey: String = "",
    @NotNull val hostPort: String = "localhost:10001",
    val notaryHostPort: String? = null
)

data class AccountPublicInfo(
    @NotNull val pubKeys: List<String> = emptyList(),
    @NotNull val domainId: String? = null,
    @NotNull val accountName: String? = null,
    val quorum: Int = 1
) {
    val id = "$accountName@$domainId"
}
