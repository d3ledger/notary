package vacuum

import config.EthereumConfig
import config.IrohaConfig
import config.IrohaCredentialConfig

interface RelayVacuumConfig {

    /** Iroha account that has registered wallets */
    val registrationServiceIrohaAccount: String

    /** Iroha account that stores tokens */
    val tokenStorageAccount: String

    /** Notary Iroha account that stores relay register */
    val notaryIrohaAccount: String

    /** Iroha configurations */
    val iroha: IrohaConfig

    /** Ethereum configurations */
    val ethereum: EthereumConfig

    val vacuumCredential: IrohaCredentialConfig

}
