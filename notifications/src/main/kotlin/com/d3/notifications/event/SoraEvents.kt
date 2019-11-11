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
        fun map(transferNotifyEvent: DepositTransferEvent) = SoraDepositEvent(
            accountIdToNotify = transferNotifyEvent.accountIdToNotify,
            amount = transferNotifyEvent.amount,
            assetName = transferNotifyEvent.assetName,
            id = transferNotifyEvent.id,
            time = transferNotifyEvent.time,
            from = transferNotifyEvent.from
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
    val fee: Fee?,
    val sideChainFee: BigDecimal?
) : SoraEvent(id, time) {
    companion object {
        fun map(transferNotifyEvent: WithdrawalTransferEvent) = SoraWithdrawalEvent(
            accountIdToNotify = transferNotifyEvent.accountIdToNotify,
            amount = transferNotifyEvent.amount,
            assetName = transferNotifyEvent.assetName,
            to = transferNotifyEvent.to,
            id = transferNotifyEvent.id,
            time = transferNotifyEvent.time,
            fee = Fee.map(transferNotifyEvent.fee),
            sideChainFee = transferNotifyEvent.sideChainFee
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
            accountIdToNotify = registrationNotifyEvent.accountId,
            address = registrationNotifyEvent.address,
            subsystem = registrationNotifyEvent.subsystem.name,
            id = registrationNotifyEvent.id,
            time = registrationNotifyEvent.time
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
            accountIdToNotify = registrationNotifyEvent.accountId,
            subsystem = registrationNotifyEvent.subsystem.name,
            id = registrationNotifyEvent.id,
            time = registrationNotifyEvent.time
        )
    }
}
