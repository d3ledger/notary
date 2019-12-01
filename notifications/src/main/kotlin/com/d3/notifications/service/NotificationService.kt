/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.service

import com.d3.notifications.event.*
import com.github.kittinunf.result.Result

/**
 * Notification service interface
 */
interface NotificationService {
    /**
     * Notifies client about deposit event
     * @param transferNotifyEvent - transfer event
     * @return result of operation
     * */
    fun notifyDeposit(transferNotifyEvent: DepositTransferEvent): Result<Unit, Exception>

    /**
     * Notifies client about sent transfer
     * @param transferNotifyEvent - transfer event
     * @return result of operation
     * */
    fun notifySendToClient(transferNotifyEvent: Client2ClientSendTransferEvent): Result<Unit, Exception>

    /**
     * Notifies client about received transfer
     * @param transferNotifyEvent - transfer event
     * @return result of operation
     * */
    fun notifyReceiveFromClient(transferNotifyEvent: Client2ClientReceiveTransferEvent): Result<Unit, Exception>

    /**
     * Notifies client about registration event
     * @param registrationNotifyEvent - registration event
     * @return result of operation
     */
    fun notifyRegistration(registrationNotifyEvent: RegistrationNotifyEvent): Result<Unit, Exception>

    /**
     * Notifies client about failed registration event
     * @param failedRegistrationNotifyEvent - failed registration event
     * @return result of operation
     */
    fun notifyFailedRegistration(failedRegistrationNotifyEvent: FailedRegistrationNotifyEvent): Result<Unit, Exception>

    /**
     * Returns the current service id
     */
    fun serviceId() = this.javaClass.canonicalName!!
}
