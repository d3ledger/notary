package registration.btc.key

import config.IrohaConfig

interface BtcPkPreGenConfig {

    val pkTriggerAccount: String

    val iroha: IrohaConfig

    val btcWalletFilePath: String
}
