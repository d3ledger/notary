package notary.btc.config

import config.BitcoinConfig
import config.IrohaConfig

/** Configuration of Bitcoin notary */
interface BtcNotaryConfig {
    /** Web port for health checks */
    val healthCheckPort: Int

    val iroha: IrohaConfig

    val bitcoin: BitcoinConfig

    /** Iroha account to store notary peer list  */
    val notaryListStorageAccount: String

    /** Iroha account to set notary peer list */
    val notaryListSetterAccount: String

    val registrationAccount: String
}
