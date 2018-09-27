package integration

import config.EthereumConfig
import config.IrohaConfig

interface TestConfig {
    val notaryIrohaAccount: String
    val tokenStorageAccount: String
    val registrationIrohaAccount: String
    val whitelistSetter: String
    val ethTestAccount: String
    val ethereum: EthereumConfig
    val iroha: IrohaConfig
}
