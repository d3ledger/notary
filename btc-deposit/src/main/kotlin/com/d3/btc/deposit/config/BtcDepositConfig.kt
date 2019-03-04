package com.d3.btc.deposit.config

import config.BitcoinConfig
import config.IrohaConfig
import config.IrohaCredentialConfig

/** Configuration of Bitcoin deposit */
interface BtcDepositConfig {
    /** Web port for health checks */
    val healthCheckPort: Int

    val iroha: IrohaConfig

    val bitcoin: BitcoinConfig

    val notaryCredential: IrohaCredentialConfig

    /** Iroha account to store notary peer list  */
    val notaryListStorageAccount: String

    /** Iroha account to set notary peer list */
    val notaryListSetterAccount: String

    val registrationAccount: String

    val btcTransferWalletPath: String
}
