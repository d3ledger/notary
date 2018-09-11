package notary.btc

import config.BitcoinConfig
import config.IrohaConfig

/** Configuration of Bitcoin notary */
interface BtcNotaryConfig {
    val iroha: IrohaConfig
    val bitcoin: BitcoinConfig
    val mstRegistrationAccount: String
}
