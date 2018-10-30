package withdrawal.btc.config

import config.IrohaConfig
import config.IrohaCredentialConfig

interface BtcWithdrawalConfig {
    // Web port for health check service
    val healthCheckPort: Int
    // Account that handles withdrawal events
    val withdrawalCredential: IrohaCredentialConfig
    // Iroha configurations
    val iroha: IrohaConfig
}
