package com.d3.registration

import config.IrohaConfig
import config.IrohaCredentialConfig

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
}
