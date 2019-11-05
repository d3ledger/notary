/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.service

import com.d3.notifications.event.RegistrationNotifyEvent
import com.d3.notifications.event.TransferNotifyEvent
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

    /**
     * Notifies client about registration event
     * @param registrationNotifyEvent - registration event
     * @return result of operation
     */
    fun notifyRegistration(registrationNotifyEvent: RegistrationNotifyEvent): Result<Unit, Exception>
}
