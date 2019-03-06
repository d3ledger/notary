package com.d3.commons.registration

import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialConfig

/**
 * Interface represents configs for registration service for cfg4k
 */
interface NotaryRegistrationConfig {

    /** Registration service port */
    val port: Int

    /** Iroha configs */
    val iroha: IrohaConfig

    /** Iroha registration service credential */
    val registrationCredential: IrohaCredentialConfig

    /** Iroha account to store clients */
    val clientStorageAccount: String
}
