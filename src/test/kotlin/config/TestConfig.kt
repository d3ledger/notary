package config

import config.DatabaseConfig
import config.EthereumConfig
import config.IrohaConfig

interface TestConfig {
    val notaryIrohaAccount: String
    val relayRegistrationIrohaAccount: String
    val registrationIrohaAccount: String
    val ethereum: EthereumConfig
    val iroha: IrohaConfig
    val db: DatabaseConfig
}
