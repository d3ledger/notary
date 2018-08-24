package notary

import config.BitcoinConfig
import config.DatabaseConfig
import config.EthereumConfig
import config.IrohaConfig

/** Configuration of refund endpoint in Notary */
interface RefundConfig {
    val port: Int
    val endpointEthereum: String
}

/** Configuration of notary */
interface NotaryConfig {
    /** Iroha account that has registered wallets */
    val registrationServiceIrohaAccount: String
    /** Iroha account that stores tokens */
    val tokenStorageAccount: String
    /** Iroha account that set whitelist for client */
    val whitelistSetter: String
    val refund: RefundConfig
    val iroha: IrohaConfig
    val ethereum: EthereumConfig
    val bitcoin: BitcoinConfig
}
