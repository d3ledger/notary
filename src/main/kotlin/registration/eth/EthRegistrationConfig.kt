package registration.eth

import config.EthereumConfig
import config.IrohaConfig
import config.IrohaCredentialConfig

/**
 * Interface represents configs for registration service for cfg4k
 */
interface EthRegistrationConfig {
    /** Port of registration service */
    val port: Int

    /** Iroha account of relay registration service */
    val relayRegistrationIrohaAccount: String

    /** Iroha account of relay account register */
    val notaryIrohaAccount: String

    val registrationCredential: IrohaCredentialConfig

    /** Iroha configuration */
    val iroha: IrohaConfig

    val ethRelayRegistryAddress: String

    val ethereum: EthereumConfig
}
