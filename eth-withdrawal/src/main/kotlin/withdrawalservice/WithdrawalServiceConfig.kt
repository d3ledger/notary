package withdrawalservice

import config.EthereumConfig
import config.IrohaConfig
import config.IrohaCredentialConfig

/** Configuration of withdrawal service */
interface WithdrawalServiceConfig {

    /** Notary account in Iroha */
    val notaryIrohaAccount: String

    /** Iroha account that stores tokens */
    val tokenStorageAccount: String

    /** Iroha account sets tokens */
    val tokenSetterAccount: String

    /** Notary storage account in Iroha */
    val notaryListStorageAccount: String

    /** Account who sets account details */
    val notaryListSetterAccount: String

    /** Notary account in Iroha */
    val registrationIrohaAccount: String

    val withdrawalCredential: IrohaCredentialConfig

    /** Iroha configuration */
    val iroha: IrohaConfig

    /** Ethereum config */
    val ethereum: EthereumConfig
}
