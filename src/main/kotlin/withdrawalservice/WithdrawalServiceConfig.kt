package withdrawalservice

import config.EthereumConfig
import config.IrohaConfig

/** Configuration of withdrawal service */
interface WithdrawalServiceConfig {

    /** Notary account in Iroha */
    val notaryIrohaAccount: String

    /** Iroha account that stores tokens */
    val tokenStorageAccount: String

    /** Notary account in Iroha */
    val registrationIrohaAccount: String

    /** Iroha configuration */
    val iroha: IrohaConfig

    /** Ethereum config */
    val ethereum: EthereumConfig
}
