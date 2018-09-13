package token

import config.IrohaConfig

interface ERC20TokenRegistrationConfig {
    //Iroha config
    val iroha: IrohaConfig
    //Path to file full of tokens in json format
    val tokensFilePath: String
    //Notary account
    val notaryIrohaAccount: String
    //Account that sets tokens in notary account
    val tokenStorageAccount: String
}
