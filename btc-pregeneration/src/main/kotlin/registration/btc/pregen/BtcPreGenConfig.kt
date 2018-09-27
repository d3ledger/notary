package registration.btc.pregen

import config.IrohaConfig

interface BtcPreGenConfig {

    /*
    Account for triggering.
    Triggering this account means starting BTC addresses pregeneration
    */
    val pubKeyTriggerAccount: String

    val notaryAccount: String

    //Iroha config
    val iroha: IrohaConfig

    //Path to BTC wallet file
    val btcWalletFilePath: String

    //Account that is used to register BTC addresses
    val registrationAccount: String


    //Account that is used to register BTC addresses in MST fashion
    val mstRegistrationAccount: String

    //Account that stores all registered notaries
    val notaryListStorageAccount: String

    //Account that sets registered notaries
    val notaryListSetterAccount: String
}
