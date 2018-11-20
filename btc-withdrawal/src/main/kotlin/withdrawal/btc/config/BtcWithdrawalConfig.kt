package withdrawal.btc.config

import config.BitcoinConfig
import config.IrohaConfig
import config.IrohaCredentialConfig

interface BtcWithdrawalConfig {
    // Web port for health check service
    val healthCheckPort: Int
    // Account that handles withdrawal events
    val withdrawalCredential: IrohaCredentialConfig
    // Account that is used to deal with registered accounts
    val registrationCredential: IrohaCredentialConfig
    // Account that stores created addresses
    val mstRegistrationAccount: String
    // Iroha configurations
    val iroha: IrohaConfig
    //Bitcoin configurations
    val bitcoin: BitcoinConfig

    val notaryCredential: IrohaCredentialConfig
}
