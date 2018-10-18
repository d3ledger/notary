package registration.btc.config

import config.IrohaConfig
import config.IrohaCredentialConfig

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

    /** Port for health check service */
    val healthCheckPort: Int
}
