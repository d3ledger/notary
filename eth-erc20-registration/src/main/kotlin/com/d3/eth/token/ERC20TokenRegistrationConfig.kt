package com.d3.eth.token

import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialConfig

interface ERC20TokenRegistrationConfig {

    /** Iroha credentials */
    val irohaCredential: IrohaCredentialConfig

    /** Iroha config */
    val iroha: IrohaConfig

    /** Path to file full of tokens anchored in Ethereum in json format */
    val ethAnchoredTokensFilePath: String

    /** Path to file full of tokens anchored in Iroha in json format */
    val irohaAnchoredTokensFilePath: String

    /** Account that stores Ethereum anchored tokens */
    val ethAnchoredTokenStorageAccount: String

    /** Account that stores Iroha anchored tokens */
    val irohaAnchoredTokenStorageAccount: String
}
