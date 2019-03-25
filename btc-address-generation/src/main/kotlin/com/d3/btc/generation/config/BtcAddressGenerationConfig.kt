package com.d3.btc.generation.config

import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialConfig

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
    val btcKeysWalletPath: String

    //TODO the only purpose of this account is creating PeerListProvider. This account must be removed from config.
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

    //Minimum number of free addresses to keep in Iroha
    val threshold: Int

    // Node id 
    val nodeId: String
}
