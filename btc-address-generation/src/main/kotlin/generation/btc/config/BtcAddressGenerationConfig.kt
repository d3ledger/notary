package generation.btc.config

import config.IrohaConfig
import config.IrohaCredentialConfig

interface BtcAddressGenerationConfig {
    /*
    Account for triggering.
    Triggering this account means starting BTC addresses generation
    */
    val pubKeyTriggerAccount: String

    val notaryAccount: String

    //Iroha config
    val iroha: IrohaConfig

    //Path to BTC wallet file
    val btcWalletFilePath: String

    //Account that is used to register BTC addresses
    val registrationAccount: IrohaCredentialConfig

    //Account that is used to register BTC addresses in MST fashion
    val mstRegistrationAccount: IrohaCredentialConfig

    //Account that stores all registered notaries
    val notaryListStorageAccount: String

    //Account that stores change addresses
    val changeAddressesStorageAccount: String

    //Account that sets registered notaries
    val notaryListSetterAccount: String

    //Port of health check service
    val healthCheckPort: Int
}
