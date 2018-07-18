package registration

import config.IrohaConfig

/**
 * Interface represents configs for registration service for cfg4k
 */
interface RegistrationConfig {
    /** Port of registration service */
    val port: Int

    /** Iroha account of relay registration service */
    val relayRegistrationIrohaAccount: String

    /** Iroha account of relay account register */
    val notaryIrohaAccount: String

    /** Iroha configuration */
    val iroha: IrohaConfig
}
