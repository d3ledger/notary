/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.event

import com.google.gson.Gson
import java.math.BigDecimal

private val gson = Gson()

/**
 * Basic event class
 * @param id - event id
 * @param txTime - event time in milliseconds
 * @param blockNum - number of block
 * @param txIndex - index of tx
 */
open class BasicEvent(val id: String, val txTime: Long, val blockNum: Long, val txIndex: Int) {
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
 * @param txTime - event time in milliseconds
 */
open class TransferNotifyEvent(
    val accountIdToNotify: String,
    val amount: BigDecimal,
    val assetName: String,
    id: String,
    txTime: Long,
    blockNum: Long,
    txIndex: Int
) : BasicEvent(id = id, txTime = txTime, blockNum = blockNum, txIndex = txIndex)

/**
 * Client to client 'send' transfer event
 */
class Client2ClientSendTransferEvent(
    accountIdToNotify: String,
    amount: BigDecimal,
    assetName: String,
    id: String,
    txTime: Long,
    blockNum: Long,
    txIndex: Int,
    val description: String,
    val to: String,
    val fee: TransferFee?
) : TransferNotifyEvent(
    accountIdToNotify = accountIdToNotify,
    amount = amount,
    assetName = assetName,
    id = id,
    txTime = txTime,
    blockNum = blockNum,
    txIndex = txIndex
)

/**
 * Client to client 'receive' transfer event
 */
class Client2ClientReceiveTransferEvent(
    accountIdToNotify: String,
    amount: BigDecimal,
    assetName: String,
    id: String,
    txTime: Long,
    blockNum: Long,
    txIndex: Int,
    val description: String,
    val from: String,
    val fee: TransferFee?
) : TransferNotifyEvent(
    accountIdToNotify = accountIdToNotify,
    amount = amount,
    assetName = assetName,
    id = id,
    txTime = txTime,
    blockNum = blockNum,
    txIndex = txIndex
)

/**
 * Deposit event
 */
class DepositTransferEvent(
    accountIdToNotify: String,
    amount: BigDecimal,
    assetName: String,
    id: String,
    txTime: Long,
    blockNum: Long,
    txIndex: Int,
    val from: String
) : TransferNotifyEvent(
    accountIdToNotify = accountIdToNotify,
    amount = amount,
    assetName = assetName,
    id = id,
    txTime = txTime,
    blockNum = blockNum,
    txIndex = txIndex
)

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
 * @param txTime - event time in milliseconds
 */
class RegistrationNotifyEvent(
    val subsystem: RegistrationEventSubsystem,
    val accountId: String,
    val address: String,
    id: String,
    txTime: Long,
    blockNum: Long,
    txIndex: Int
) : BasicEvent(id = id, txTime = txTime, blockNum = blockNum, txIndex = txIndex)

/**
 * Data class that holds failed registration event data
 * @param subsystem - type of registration
 * @param accountId - registered account id
 * @param id - event id
 * @param txTime - event time in milliseconds
 */
class FailedRegistrationNotifyEvent(
    val subsystem: RegistrationEventSubsystem,
    val accountId: String,
    id: String,
    txTime: Long,
    blockNum: Long,
    txIndex: Int
) : BasicEvent(id = id, txTime = txTime, blockNum = blockNum, txIndex = txIndex)

enum class RegistrationEventSubsystem {
    ETH
}

/**
 * Class that holds information about withdrawal proofs
 * @param accountIdToNotify - account to notify
 * @param tokenContractAddress - TODO describe
 * @param amount - amount of asset to withdraw
 * @param relay - TODO describe
 * @param proofs - withdrawal proofs
 * @param irohaTxHash - original withdrawal Iroha tx hash
 * @param to - Ethereum address of receiver
 * @param id - identifier of event
 * @param txTime - time of event
 */
class EthWithdrawalProofsEvent(
    val accountIdToNotify: String,
    val tokenContractAddress: String,
    val amount: BigDecimal,
    val relay: String,
    val proofs: List<ECDSASignature>,
    val irohaTxHash: String,
    val to: String,
    id: String,
    txTime: Long,
    blockNum: Long,
    txIndex: Int
) : BasicEvent(id = id, txTime = txTime, blockNum = blockNum, txIndex = txIndex)

data class ECDSASignature(
    val r: String,
    val s: String,
    val v: String
)

/**
 * Class that holds information about acknowledged(used) Eth withdrawal proof event
 * @param proofEventId - id of acknowledged event
 * @param id - identifier of event
 * @param txTime - time of event
 */
class AckEthWithdrawalProofEvent(
    val proofEventId: String,
    id: String,
    txTime: Long,
    blockNum: Long,
    txIndex: Int
) : BasicEvent(id = id, txTime = txTime, blockNum = blockNum, txIndex = txIndex)
