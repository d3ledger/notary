/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.event

import com.d3.commons.util.GsonInstance
import java.math.BigDecimal

/**
 * The file contains data transfer objects for Sora notification REST service
 */

private val gson = GsonInstance.get()

/**
 * Sora event
 * @param id - event id
 * @param time - event time
 */
open class SoraEvent(val id: String, val time: Long) {
    override fun toString(): String {
        return gson.toJson(this)
    }
}

class SoraDepositEvent(
    val accountIdToNotify: String,
    val amount: BigDecimal,
    val assetName: String,
    id: String,
    time: Long,
    val from: String?
) : SoraEvent(id, time) {
    companion object {
        fun map(transferNotifyEvent: TransferNotifyEvent) = SoraDepositEvent(
            transferNotifyEvent.accountIdToNotify,
            transferNotifyEvent.amount,
            transferNotifyEvent.assetName,
            transferNotifyEvent.id,
            transferNotifyEvent.time,
            transferNotifyEvent.from
        )
    }
}

class SoraWithdrawalEvent(
    val accountIdToNotify: String,
    val amount: BigDecimal,
    val assetName: String,
    val to: String,
    id: String,
    time: Long,
    val fee: Fee?
) : SoraEvent(id, time) {
    companion object {
        fun map(transferNotifyEvent: TransferNotifyEvent) = SoraWithdrawalEvent(
            transferNotifyEvent.accountIdToNotify,
            transferNotifyEvent.amount,
            transferNotifyEvent.assetName,
            transferNotifyEvent.to!!,
            transferNotifyEvent.id,
            transferNotifyEvent.time,
            Fee.map(transferNotifyEvent.fee)
        )
    }
}

data class Fee(val amount: BigDecimal, val assetName: String) {
    companion object {
        fun map(transferFee: TransferFee?): Fee? {
            return if (transferFee == null) {
                null
            } else {
                Fee(transferFee.amount, transferFee.assetName)
            }
        }
    }
}

class SoraRegistrationEvent(
    val accountIdToNotify: String,
    val address: String,
    val subsystem: String,
    id: String,
    time: Long
) : SoraEvent(id, time) {
    companion object {
        fun map(registrationNotifyEvent: RegistrationNotifyEvent) = SoraRegistrationEvent(
            registrationNotifyEvent.accountId,
            registrationNotifyEvent.address,
            registrationNotifyEvent.subsystem.name,
            registrationNotifyEvent.id,
            registrationNotifyEvent.time
        )
    }
}

class SoraFailedRegistrationEvent(
    val accountIdToNotify: String,
    val subsystem: String,
    id: String,
    time: Long
) : SoraEvent(id, time) {
    companion object {
        fun map(registrationNotifyEvent: FailedRegistrationNotifyEvent) = SoraFailedRegistrationEvent(
            registrationNotifyEvent.accountId,
            registrationNotifyEvent.subsystem.name,
            registrationNotifyEvent.id,
            registrationNotifyEvent.time
        )
    }
}
