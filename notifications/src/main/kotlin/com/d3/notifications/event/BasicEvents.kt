/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.event

import com.d3.commons.util.GsonInstance
import java.math.BigDecimal
import java.math.BigInteger

private val gson = GsonInstance.get()

/**
 * Basic event class
 * @param id - event id
 * @param time - event time in milliseconds
 */
open class BasicEvent(val id: String, val time: Long) {
    override fun toString(): String {
        return gson.toJson(this)
    }
}

/**
 * Data class that holds transfer event data
 *
 * @param accountIdToNotify - account id that will be notified
 * @param amount - transfer amount
 * @param assetName - name of asset
 * @param id - event id
 * @param time - event time in milliseconds
 */
open class TransferNotifyEvent(
    val accountIdToNotify: String,
    val amount: BigDecimal,
    val assetName: String,
    id: String,
    time: Long
) : BasicEvent(id, time)

/**
 * Client to client 'send' transfer event
 */
class Client2ClientSendTransferEvent(
    accountIdToNotify: String,
    amount: BigDecimal,
    assetName: String,
    id: String,
    time: Long,
    val description: String,
    val to: String,
    val fee: TransferFee?
) : TransferNotifyEvent(accountIdToNotify, amount, assetName, id, time)

/**
 * Client to client 'receive' transfer event
 */
class Client2ClientReceiveTransferEvent(
    accountIdToNotify: String,
    amount: BigDecimal,
    assetName: String,
    id: String,
    time: Long,
    val description: String,
    val from: String,
    val fee: TransferFee?
) : TransferNotifyEvent(accountIdToNotify, amount, assetName, id, time)


/**
 * Deposit event
 */
class DepositTransferEvent(
    accountIdToNotify: String,
    amount: BigDecimal,
    assetName: String,
    id: String,
    time: Long,
    val from: String
) : TransferNotifyEvent(accountIdToNotify, amount, assetName, id, time)

/**
 * Withdrawal event
 */
class WithdrawalTransferEvent(
    accountIdToNotify: String,
    amount: BigDecimal,
    assetName: String,
    id: String,
    time: Long,
    val to: String,
    val fee: TransferFee?,
    val sideChainFee: BigDecimal?
) : TransferNotifyEvent(accountIdToNotify, amount, assetName, id, time)

/**
 * Rollback event
 */
class RollbackTransferEvent(
    accountIdToNotify: String,
    amount: BigDecimal,
    assetName: String,
    id: String,
    time: Long,
    val fee: TransferFee?
) : TransferNotifyEvent(accountIdToNotify, amount, assetName, id, time)

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

/**
 * Data class that holds failed registration event data
 * @param subsystem - type of registration
 * @param accountId - registered account id
 * @param id - event id
 * @param time - event time in milliseconds
 */
class FailedRegistrationNotifyEvent(
    val subsystem: RegistrationEventSubsystem,
    val accountId: String,
    id: String,
    time: Long
) : BasicEvent(id, time)

enum class RegistrationEventSubsystem {
    ETH, BTC
}

/**
 * Class that holds information about withdrawal proofs
 * @param accountIdToNotify - account to notify
 * @param tokenContractAddress - TODO describe
 * @param amount - amount of asset to withdraw
 * @param relay - TODO describe
 * @param proofs - withdrawal proofs
 * @param irohaTxHash - original withdrawal Iroha tx hash
 * @param id - identifier of event
 * @param time - time of event
 */
class EthWithdrawalProofsEvent(
    val accountIdToNotify: String,
    val tokenContractAddress: String,
    val amount: BigDecimal,
    val relay: String,
    val proofs: List<ECDSASignature>,
    val irohaTxHash: String,
    id: String,
    time: Long
) : BasicEvent(id, time)

data class ECDSASignature(
    val r: String,
    val s: String,
    val v: BigInteger
)
