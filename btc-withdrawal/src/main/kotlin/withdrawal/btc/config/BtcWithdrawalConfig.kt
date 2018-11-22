package withdrawal.btc.config

import config.BitcoinConfig
import config.IrohaConfig
import config.IrohaCredentialConfig

interface BtcWithdrawalConfig {
    // Web port for health check service
    val healthCheckPort: Int
    // Account that handles withdrawal events
    val withdrawalCredential: IrohaCredentialConfig
    // Account that stores registered addresses
    val registrationAccount: String
    // Iroha configurations
    val iroha: IrohaConfig
    //Bitcoin configurations
    val bitcoin: BitcoinConfig
    // Base58 formatted Bitcoin address that stores change
    val changeAddress: String

    val notaryCredential: IrohaCredentialConfig
}
