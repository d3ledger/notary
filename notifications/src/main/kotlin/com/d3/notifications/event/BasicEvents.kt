/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.event

import java.math.BigDecimal

/**
 * Basic event class
 * @param id - event id
 * @param time - event time in milliseconds
 */
open class BasicEvent(val id: String, val time: Long)

/**
 * Data class that holds transfer event data
 *
 * @param accountIdToNotify - account id that will be notified
 * @param type - type of event
 * @param amount - transfer amount
 * @param assetName - name of asset
 * @param id - event id
 * @param time - event time in milliseconds
 * @param description - description of transfer
 * @param from - defines the origin of transfer. null by default
 * @param to - defines the destination of transfer. null by default.
 * @param fee - fee to pay for transfer
 */
class TransferNotifyEvent(
    val type: TransferEventType,
    val accountIdToNotify: String,
    val amount: BigDecimal,
    val assetName: String,
    id: String,
    time: Long,
    val description: String = "",
    val from: String? = null,
    val to: String? = null,
    val fee: TransferFee? = null
) : BasicEvent(id, time)

/**
 * Type of transfer
 */
enum class TransferEventType {
    DEPOSIT, ROLLBACK, WITHDRAWAL, TRANSFER_RECEIVE, TRANSFER_SEND
}

/**
 * Fee object
 * @param amount - amount of fee
 * @param assetName - fee currency
 */
data class TransferFee(val amount: BigDecimal, val assetName: String)

/**
 * Data class that holds registration event data
 * @param subsystem - type of registration
 * @param accountId - registered account id
 * @param address - registered address
 * @param id - event id
 * @param time - event time in milliseconds
 */
class RegistrationNotifyEvent(
    val subsystem: RegistrationEventSubsystem,
    val accountId: String,
    val address: String,
    id: String,
    time: Long
) : BasicEvent(id, time)

enum class RegistrationEventSubsystem {
    ETH, BTC
}
