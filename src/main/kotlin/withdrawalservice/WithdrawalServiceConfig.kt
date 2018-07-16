package withdrawalservice

import config.IrohaConfig

/** Configuration of withdrawal service */
interface WithdrawalServiceConfig {

    /** Iroha configuration */
    val iroha: IrohaConfig
}
