package com.d3.eth.vacuum

import com.d3.commons.config.EthereumConfig
import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialConfig

interface RelayVacuumConfig {

    /** Iroha account that has registered wallets */
    val registrationServiceIrohaAccount: String

    /** Iroha account that stores Ethereum anchored ERC20 tokens */
    val ethAnchoredTokenStorageAccount: String

    /** Iroha account that sets that stores Ethereum anchored ERC20 tokens */
    val ethAnchoredTokenSetterAccount: String


    /** Iroha account that stores Iroha anchored ERC20 tokens */
    val irohaAnchoredTokenStorageAccount: String

    /** Iroha account that sets that stores Iroha anchored ERC20 tokens */
    val irohaAnchoredTokenSetterAccount: String

    /** Notary Iroha account that stores relay register */
    val notaryIrohaAccount: String

    /** Iroha configurations */
    val iroha: IrohaConfig

    /** Ethereum configurations */
    val ethereum: EthereumConfig

    val vacuumCredential: IrohaCredentialConfig

}
