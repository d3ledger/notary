/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.bootstrap.dto

import java.math.BigInteger
import javax.validation.constraints.NotNull

data class SigsData(val vv: ArrayList<BigInteger>, val rr: ArrayList<ByteArray>, val ss: ArrayList<ByteArray>)

data class UpdateMasterContractResponse(
    val success:Boolean = false
) : Conflictable() {
    constructor(errorCode: String? = null, message: String? = null) :
            this(false) {
        this.errorCode = errorCode
        this.message = message
    }
}

data class UpdateMasterContractRequest(
    @NotNull val network: EthereumNetworkProperties = EthereumNetworkProperties(),
    @NotNull val masterContract: MasterContractProperties = MasterContractProperties(),
    val newPeerAddress: String? = null,
    val removePeerAddress: String? = null
)

data class MasterContractProperties(
    @NotNull val address: String? = null,
    @NotNull val notaries: List<StringKeyPair> = emptyList()
)

data class MasterContractsRequest(
    @NotNull val network: EthereumNetworkProperties = EthereumNetworkProperties(),
    @NotNull val notaryEthereumAccounts: List<String> = emptyList()
)

data class StringKeyPair(
    @NotNull val private: String = "",
    @NotNull val public: String = ""
)

data class MasterContractResponse(
    val masterEthAddress: String? = null,
    val relayRegistryEthAddress: String? = null,
    val relayImplementationAddress: String? = null,
    val soraTokenEthAddress: String? = null
) : Conflictable() {
    constructor(errorCode: String? = null, message: String? = null) :
            this(null, null, null, null) {
        this.errorCode = errorCode
        this.message = message
    }
}
