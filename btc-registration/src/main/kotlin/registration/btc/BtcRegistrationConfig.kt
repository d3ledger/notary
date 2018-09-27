package registration.btc

import config.IrohaConfig

/**
 * Interface represents configs for registration service for cfg4k
 */
interface BtcRegistrationConfig {
    /** Port of registration service */
    val port: Int

    /** Iroha account for btc account register */
    val registrationAccount: String

    /** Iroha account for btc account register in MST fashion*/
    val mstRegistrationAccount: String

    /** Iroha configuration */
    val iroha: IrohaConfig

    val btcWalletPath: String
}
