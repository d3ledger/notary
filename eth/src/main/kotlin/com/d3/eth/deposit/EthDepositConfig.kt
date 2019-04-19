package com.d3.eth.deposit

import com.d3.commons.config.EthereumConfig
import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialConfig

/** Configuration of refund endpoint in Notary */
interface RefundConfig {
    val port: Int
}

/** Configuration of deposit */
interface EthDepositConfig {
    /** Iroha account that has registered wallets */
    val registrationServiceIrohaAccount: String

    /** Iroha account that stores Ethereum anchored ERC20 tokens */
    val ethAnchoredTokenStorageAccount: String

    /** Iroha account that sets Ethereum anchored ERC20 tokens */
    val ethAnchoredTokenSetterAccount: String

    /** Iroha account that stores Iroha anchored ERC20 tokens */
    val irohaAnchoredTokenStorageAccount: String

    /** Iroha account that sets Iroha anchored ERC20 tokens */
    val irohaAnchoredTokenSetterAccount: String

    /** Iroha account that set whitelist for client */
    val whitelistSetter: String

    val notaryCredential: IrohaCredentialConfig

    val refund: RefundConfig

    val iroha: IrohaConfig

    val ethereum: EthereumConfig

    /** Iroha withdrawal account grant permission to */
    val withdrawalAccountId: String
}
