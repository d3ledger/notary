package registration.relay

import config.DatabaseConfig
import config.EthereumConfig
import config.IrohaConfig

/**
 * Interface represents configs for relay registration service for cfg4k
 */
interface RelayRegistrationConfig {

    /** Number of accounts to deploy */
    val number: Int

    /** Address of master smart contract in Ethereum */
    val ethMasterWallet: String

    /** Notary Iroha account that stores relay register */
    val notaryIrohaAccount: String

    /** Iroha configurations */
    val iroha: IrohaConfig

    /** Ethereum configurations */
    val ethereum: EthereumConfig

    /** Db configurations */
    val db: DatabaseConfig
}
