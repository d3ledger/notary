/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.bootstrap.dto

import java.math.BigInteger
import javax.validation.constraints.NotNull

data class SigsData(
    val vv: ArrayList<BigInteger>,
    val rr: ArrayList<ByteArray>,
    val ss: ArrayList<ByteArray>
)

data class UpdateMasterContractResponse(
    val success: Boolean = false
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
    @NotNull val notaries: List<InitWalletInfo> = emptyList()
)

data class AllInitialContractsRequest(
    @NotNull val network: EthereumNetworkProperties = EthereumNetworkProperties(),
    @NotNull val notaryEthereumAccounts: List<String> = emptyList()
)

data class InitWalletInfo(
    @NotNull val password: String = "",
    @NotNull val path: String = ""
)

data class DeployInitialContractsResponse(
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

data class DeploySmartContractResponse(
    val contractAddress: String? = null
) : Conflictable() {
    constructor(errorCode: String? = null, message: String? = null) :
            this(null) {
        this.errorCode = errorCode
        this.message = message
    }
}

data class DeployMasterContractRequest(
    @NotNull val network: EthereumNetworkProperties = EthereumNetworkProperties(),
    @NotNull val notaryEthereumAccounts: List<String> = emptyList(),
    @NotNull val relayRegistryAddress: String = ""
)

data class DeployRelayImplementationRequest(
    @NotNull val network: EthereumNetworkProperties = EthereumNetworkProperties(),
    @NotNull val masterContractAddress: String = ""
)

data class DeployMasterContractResponse(
    val contractAddress: String? = null,
    val soraAddress: String? = null
) : Conflictable()
