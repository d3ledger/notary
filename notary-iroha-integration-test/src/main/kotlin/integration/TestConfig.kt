package integration

import config.EthereumConfig
import config.IrohaConfig
import config.IrohaCredentialConfig

interface TestConfig {
    val ethTestAccount: String
    val ethereum: EthereumConfig
    val iroha: IrohaConfig
    val testCredentialConfig: IrohaCredentialConfig
    val testQueue : String
}
