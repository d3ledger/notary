package com.d3.btc.registration.config

import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialConfig

/**
 * Interface represents configs for registration service for cfg4k
 */
interface BtcRegistrationConfig {
    /** Port of registration service */
    val port: Int

    /** Iroha account for btc account register */
    val registrationCredential: IrohaCredentialConfig

    /** Iroha account for btc account register in MST fashion*/
    val mstRegistrationAccount: String

    /** Notary account that stores addresses in details*/
    val notaryAccount: String

    /** Iroha configuration */
    val iroha: IrohaConfig

    /** Node id */
    val nodeId: String
}
