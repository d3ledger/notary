package vacuum

import config.DatabaseConfig
import config.EthereumConfig
import config.IrohaConfig

interface RelayVacuumConfig {

    /** Iroha account that has registered wallets */
    val registrationServiceIrohaAccount: String

    /** Notary Iroha account that stores relay register */
    val notaryIrohaAccount: String

    /** Iroha configurations */
    val iroha: IrohaConfig

    /** Ethereum configurations */
    val ethereum: EthereumConfig

    /** Db configurations */
    val db: DatabaseConfig
}
