/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.config

import com.d3.chainadapter.client.RMQConfig
import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialRawConfig

/**
 * Configuration of notification service
 */
interface NotificationsConfig {
    // Iroha configs
    val iroha: IrohaConfig
    // Notary account credential. Used to listen to Iroha blocks
    val notaryCredential: IrohaCredentialRawConfig
    // Billing account for transfers
    val transferBillingAccount: String
    // Billing account for withdrawals
    val withdrawalBillingAccount: String
    // HTTP port for the health check web-service
    val healthCheckPort: Int
    // Account that is used as a client storage
    val clientStorageAccount: String
    // Account name of registration service(no domain)
    val registrationServiceAccountName: String
    // Local RMQ configuration. Used to work with chain-adapter
    val rmq: RMQConfig
    // Queue name
    val blocksQueue: String
    // Timeout for Iroha queries
    val irohaQueryTimeoutMls: Int
    // Ethereum registration service Iroha account id
    val ethRegistrationServiceAccount: String
    // Eth withdrawal account
    val ethWithdrawalAccount: String
    // Eth deposit account
    val ethDepositAccount: String
    // Eth withdrawal proof setter account
    val ethWithdrawalProofSetter: String
}