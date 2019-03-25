/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.btc.withdrawal.config

import com.d3.commons.config.BitcoinConfig
import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialConfig

interface BtcWithdrawalConfig {
    // Web port for health check service
    val healthCheckPort: Int
    // Account that handles withdrawal events
    val withdrawalCredential: IrohaCredentialConfig
    // Account that stores Bitcoin transaction signatures
    val signatureCollectorCredential: IrohaCredentialConfig
    // Account that is used to deal with registered accounts
    val registrationCredential: IrohaCredentialConfig
    //TODO this must be removed
    // Account that stores fee rate
    val btcFeeRateCredential: IrohaCredentialConfig
    //Account that stores withdrawal consensus data
    val btcConsensusCredential:IrohaCredentialConfig
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
    // RabbitMQ queue that is used for listening to Iroha blocks
    val irohaBlockQueue: String
    // Path to wallet that stores transfers(UTXO)
    val btcTransfersWalletPath: String
    // Path to wallet that stores keys
    val btcKeysWalletPath: String
}
