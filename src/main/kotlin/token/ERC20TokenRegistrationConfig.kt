package token

import config.IrohaConfig
import config.IrohaCredentialConfig

interface ERC20TokenRegistrationConfig {

    /** Iroha credentials */
    val irohaCredential: IrohaCredentialConfig

    //Iroha config
    val iroha: IrohaConfig

    //Path to file full of tokens in json format
    val tokensFilePath: String

    //Account that stores tokens
    val tokenStorageAccount: String
}
