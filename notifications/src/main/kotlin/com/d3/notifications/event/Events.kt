/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.event

import java.math.BigDecimal

/**
 * Event marker-interface
 */
interface Event

/**
 * Data class that holds transfer event data
 *
 * @param accountId - account id that will be notified
 * @param type - type of event
 * @param amount - transfer amount
 * @param assetName - name of asset
 * @param description - description of transfer
 * @param from - defines the origin of transfer. null by default
 * @param to - defines the destination of transfer. null by default.
 */
data class TransferNotifyEvent(
    val type: TransferEventType,
    val accountId: String,
    val amount: BigDecimal,
    val assetName: String,
    val description: String,
    val from: String? = null,
    val to: String? = null
) : Event

enum class TransferEventType {
    DEPOSIT, ROLLBACK, WITHDRAWAL, TRANSFER_RECEIVE, TRANSFER_SEND
}

/**
 * Data class that holds registration event data
 * @param subsystem - type of registration
 * @param accountId - registered account id
 * @param address - registered address
 */
data class RegistrationNotifyEvent(
    val subsystem: RegistrationEventSubsystem,
    val accountId: String,
    val address: String
):Event

enum class RegistrationEventSubsystem {
    ETH, BTC
}
