package com.d3.btc.withdrawal.config

import config.BitcoinConfig
import config.IrohaConfig
import config.IrohaCredentialConfig

interface BtcWithdrawalConfig {
    // Web port for health check service
    val healthCheckPort: Int
    // Account that handles withdrawal events
    val withdrawalCredential: IrohaCredentialConfig
    // Account that stores Bitcoin transaction signatures
    val signatureCollectorCredential: IrohaCredentialConfig
    // Account that is used to deal with registered accounts
    val registrationCredential: IrohaCredentialConfig
    // Account that stores fee rate
    val btcFeeRateCredential: IrohaCredentialConfig
    // Account that stores created addresses
    val mstRegistrationAccount: String
    // Account that stores change addresses
    val changeAddressesStorageAccount: String
    // Iroha configurations
    val iroha: IrohaConfig
    // Bitcoin configurations
    val bitcoin: BitcoinConfig
    // Credentials of notary account
    val notaryCredential: IrohaCredentialConfig
    // Account that stores notaries
    val notaryListStorageAccount: String
    // Account that saves notaries into notary storage account
    val notaryListSetterAccount: String
}
