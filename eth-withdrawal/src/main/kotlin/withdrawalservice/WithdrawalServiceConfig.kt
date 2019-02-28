package withdrawalservice

import com.d3.commons.config.EthereumConfig
import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialConfig

/** Configuration of withdrawal service */
interface WithdrawalServiceConfig {
    /** Withdrawal endpoint port */
    val port: Int

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

    /** RMQ Iroha Block */
    val ethIrohaWithdrawalQueue: String
}
