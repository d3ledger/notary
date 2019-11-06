/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.event

import java.math.BigDecimal

/**
 * The file contains data transfer objects for Sora notification REST service
 */

/**
 * Sora event marker-interface
 */
interface SoraEvent

data class SoraDepositEvent(
    val accountIdToNotify: String,
    val amount: BigDecimal,
    val assetName: String,
    val from: String?
) : SoraEvent {
    companion object {
        fun map(transferNotifyEvent: TransferNotifyEvent): SoraDepositEvent {
            return SoraDepositEvent(
                transferNotifyEvent.accountIdToNotify,
                transferNotifyEvent.amount,
                transferNotifyEvent.assetName,
                transferNotifyEvent.from
            )
        }
    }
}

data class SoraWithdrawalEvent(
    val accountIdToNotify: String,
    val amount: BigDecimal,
    val assetName: String,
    val to: String,
    val fee: BigDecimal?,
    val feeAssetName: String?
) : SoraEvent {
    companion object {
        fun map(transferNotifyEvent: TransferNotifyEvent): SoraWithdrawalEvent {
            return SoraWithdrawalEvent(
                transferNotifyEvent.accountIdToNotify,
                transferNotifyEvent.amount,
                transferNotifyEvent.assetName,
                transferNotifyEvent.to!!,
                transferNotifyEvent.fee,
                transferNotifyEvent.feeAssetName
            )
        }
    }
}

data class SoraRegistrationEvent(
    val accountIdToNotify: String,
    val address: String,
    val subsystem: String
) : SoraEvent {
    companion object {
        fun map(registrationNotifyEvent: RegistrationNotifyEvent): SoraRegistrationEvent {
            return SoraRegistrationEvent(
                registrationNotifyEvent.accountId,
                registrationNotifyEvent.address,
                registrationNotifyEvent.subsystem.name
            )
        }
    }
}
