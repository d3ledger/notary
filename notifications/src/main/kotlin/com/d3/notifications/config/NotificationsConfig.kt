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
    // Notification service account credential.
    val notificationCredential: IrohaCredentialRawConfig
    // Billing account for transfers
    val transferBillingAccount: String
    // Billing account for withdrawals
    val withdrawalBillingAccount: String
    // HTTP port for web-services
    val webPort: Int
    // Account that is used as a client storage
    val clientStorageAccount: String
    // Account name of registration service(no domain)
    val registrationServiceAccountName: String
    // RMQ configuration
    val rmq: RMQConfig
    // Queue name
    val blocksQueue: String
    // Timeout for Iroha queries
    val irohaQueryTimeoutMls:Int
    // Node identifier. Must be unique for every node
    val nodeId:String
}

/**
 * SMTP configuration
 */
interface SMTPConfig {
    // SMTP host
    val host: String
    // SMTP port
    val port: Int
    // SMTP user accountId
    val userName: String
    // SMTP password
    val password: String
}

/**
 * Push API configuration
 */
interface PushAPIConfig {

    // Base64Url encoded VAPID public key
    val vapidPubKeyBase64: String

    // Base64Url encoded VAPID private key
    val vapidPrivKeyBase64: String
}
