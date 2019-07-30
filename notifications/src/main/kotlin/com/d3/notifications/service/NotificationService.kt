/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.service

import com.github.kittinunf.result.Result
import java.math.BigDecimal

/**
 * Notification service interface
 */
interface NotificationService {
    /**
     * Notifies client about deposit event
     * @param transferNotifyEvent - transfer event
     * @return result of operation
     * */
    fun notifyDeposit(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception>

    /**
     * Notifies client about withdrawal event
     * @param transferNotifyEvent - transfer event
     * @return result of operation
     * */
    fun notifyWithdrawal(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception>

    /**
     * Notifies client about sent transfer
     * @param transferNotifyEvent - transfer event
     * @return result of operation
     * */
    fun notifySendToClient(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception>

    /**
     * Notifies client about received transfer
     * @param transferNotifyEvent - transfer event
     * @return result of operation
     * */
    fun notifyReceiveFromClient(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception>

    /**
     * Notifies client about rollback event
     * @param transferNotifyEvent - transfer event
     * @return result of operation
     * */
    fun notifyRollback(transferNotifyEvent: TransferNotifyEvent): Result<Unit, Exception>
}

/**
 * Data class that holds transfer event data
 *
 * @param accountId - account id that will be notified
 * @param amount - transfer amount
 * @param assetName - name of asset
 * @param description - description of transfer
 * @param from - defines the origin of transfer. null by default
 * @param to - defines the destination of transfer. null by default.
 */
data class TransferNotifyEvent(
    val accountId: String,
    val amount: BigDecimal,
    val assetName: String,
    val description: String,
    val from: String? = null,
    val to: String? = null
)
