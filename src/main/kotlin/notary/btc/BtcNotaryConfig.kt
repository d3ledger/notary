package notary.btc

import config.BitcoinConfig
import config.IrohaConfig

/** Configuration of Bitcoin notary */
interface BtcNotaryConfig {
    val iroha: IrohaConfig

    val bitcoin: BitcoinConfig

    val mstRegistrationAccount: String

    /** Iroha account to store notary peer list  */
    val notaryListStorageAccount: String

    /** Iroha account to set notary peer list */
    val notaryListSetterAccount: String
}
