package registration.btc

import config.IrohaConfig

/**
 * Interface represents configs for registration service for cfg4k
 */
interface BtcRegistrationConfig {
    /** Port of registration service */
    val port: Int

    /** Iroha account of relay account register */
    val notaryIrohaAccount: String

    /** Iroha configuration */
    val iroha: IrohaConfig

    val btcWalletPath: String
}
