package config

interface TestConfig {
    val notaryIrohaAccount: String
    val relayRegistrationIrohaAccount: String
    val tokenStorageAccount: String
    val registrationIrohaAccount: String
    val ropstenTestAccount: String
    val whitelistSetter: String
    val ethTestAccount: String
    val ethereum: EthereumConfig
    val iroha: IrohaConfig
}
