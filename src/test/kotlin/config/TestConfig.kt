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
    val testCredentialConfig: IrohaCredentialConfig
    val bitcoin: BitcoinConfig
    val credentialsPassword: String
    val nodeLogin: String
    val nodePassword: String
}
