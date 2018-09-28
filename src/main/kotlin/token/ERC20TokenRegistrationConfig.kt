package token

import config.IrohaConfig
import config.IrohaCredentialConfig

interface ERC20TokenRegistrationConfig {
    //Iroha config
    val iroha: IrohaConfig
    //Path to file full of tokens in json format
    val tokensFilePath: String
    //Account that stores tokens
    val tokenStorageAccount: String
    //Account that sets tokens in tokenStorageAccount
    val tokenCreatorAccount: IrohaCredentialConfig
}
