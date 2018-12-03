package config

interface TestConfig {
    val ethTestAccount: String
    val ethereum: EthereumConfig
    val iroha: IrohaConfig
    val testCredentialConfig: IrohaCredentialConfig
}
