package vacuum

import com.d3.commons.config.EthereumConfig
import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialConfig

interface RelayVacuumConfig {

    /** Iroha account that has registered wallets */
    val registrationServiceIrohaAccount: String

    /** Iroha account that stores tokens */
    val tokenStorageAccount: String

    /** Iroha account that sets tokens */
    val tokenSetterAccount: String

    /** Notary Iroha account that stores relay register */
    val notaryIrohaAccount: String

    /** Iroha configurations */
    val iroha: IrohaConfig

    /** Ethereum configurations */
    val ethereum: EthereumConfig

    val vacuumCredential: IrohaCredentialConfig

}
