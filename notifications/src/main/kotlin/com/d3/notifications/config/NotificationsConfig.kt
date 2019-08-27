/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.config

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
    // SMTP config
    val smtp: SMTPConfig
    // Push API config
    val push: PushAPIConfig
    // Billing account for transfers
    val transferBillingAccount: String
    // Billing account for withdrawals
    val withdrawalBillingAccount: String
    // HTTP port for web-services
    val webPort: Int
    // Account that is used as a client storage
    val clientStorageAccount:String
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
