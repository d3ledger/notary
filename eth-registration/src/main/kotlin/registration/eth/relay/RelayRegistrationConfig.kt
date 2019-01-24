package registration.eth.relay

import config.EthereumConfig
import config.IrohaConfig
import config.IrohaCredentialConfig

/**
 * Interface represents configs for relay registration service for cfg4k
 */
interface RelayRegistrationConfig {

    /** Number of accounts to deploy */
    val number: Int

    /** How often run registration of new relays in seconds */
    val replenishmentPeriod: Long

    /** Address of master smart contract in Ethereum */
    val ethMasterWallet: String

    /** Notary Iroha account that stores relay register */
    val notaryIrohaAccount: String

    val relayRegistrationCredential: IrohaCredentialConfig

    /** Iroha configurations */
    val iroha: IrohaConfig

    /** Ethereum configurations */
    val ethereum: EthereumConfig
}
