package com.d3.eth.registration.relay

import com.d3.commons.config.EthereumConfig
import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialConfig

/**
 * Interface represents configs for relay registration service for cfg4k
 */
interface RelayRegistrationConfig {

    /** Number of accounts to deploy */
    val number: Int

    /** How often run registration of new relays in seconds */
    val replenishmentPeriod: Long

    /** Address of master smart contract in Ethereum */
    val ethMasterWallet: String

    /** Address of implementation of Relay contract in Ethereum */
    val ethRelayImplementationAddress: String

    /** Notary Iroha account that stores relay register */
    val notaryIrohaAccount: String

    val relayRegistrationCredential: IrohaCredentialConfig

    /** Iroha configurations */
    val iroha: IrohaConfig

    /** Ethereum configurations */
    val ethereum: EthereumConfig
}
