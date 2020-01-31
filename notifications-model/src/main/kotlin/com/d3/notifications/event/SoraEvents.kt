/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.event

import com.google.gson.Gson
import java.math.BigDecimal

/**
 * The file contains data transfer objects for Sora notification REST service
 */

private val gson = Gson()

/**
 * Sora event
 * @param id - event id
 * @param txTime - transaction time
 * @param blockNum - number of block
 * @param txIndex - index of tx
 */
open class SoraEvent(val id: String, val txTime: Long, val blockNum: Long, val txIndex: Int) {

    fun toJson() = gson.toJson(this)!!

    override fun toString(): String = this.toJson()
}

class SoraDepositEvent(
    val accountIdToNotify: String,
    val amount: BigDecimal,
    val assetName: String,
    id: String,
    txTime: Long,
    blockNum: Long,
    txIndex: Int,
    val from: String?
) : SoraEvent(id = id, txTime = txTime, blockNum = blockNum, txIndex = txIndex) {
    companion object {
        fun map(transferNotifyEvent: DepositTransferEvent) = SoraDepositEvent(
            accountIdToNotify = transferNotifyEvent.accountIdToNotify,
            amount = transferNotifyEvent.amount,
            assetName = transferNotifyEvent.assetName,
            id = transferNotifyEvent.id,
            txTime = transferNotifyEvent.txTime,
            blockNum = transferNotifyEvent.blockNum,
            txIndex = transferNotifyEvent.txIndex,
            from = transferNotifyEvent.from
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
    txTime: Long,
    blockNum: Long,
    txIndex: Int
) : SoraEvent(id = id, txTime = txTime, blockNum = blockNum, txIndex = txIndex) {
    companion object {
        fun map(registrationNotifyEvent: RegistrationNotifyEvent) = SoraRegistrationEvent(
            accountIdToNotify = registrationNotifyEvent.accountId,
            address = registrationNotifyEvent.address,
            subsystem = registrationNotifyEvent.subsystem.name,
            id = registrationNotifyEvent.id,
            txTime = registrationNotifyEvent.txTime,
            blockNum = registrationNotifyEvent.blockNum,
            txIndex = registrationNotifyEvent.txIndex
        )
    }
}

class SoraFailedRegistrationEvent(
    val accountIdToNotify: String,
    val subsystem: String,
    id: String,
    txTime: Long,
    blockNum: Long,
    txIndex: Int
) : SoraEvent(id = id, txTime = txTime, blockNum = blockNum, txIndex = txIndex) {
    companion object {
        fun map(registrationNotifyEvent: FailedRegistrationNotifyEvent) = SoraFailedRegistrationEvent(
            accountIdToNotify = registrationNotifyEvent.accountId,
            subsystem = registrationNotifyEvent.subsystem.name,
            id = registrationNotifyEvent.id,
            txTime = registrationNotifyEvent.txTime,
            txIndex = registrationNotifyEvent.txIndex,
            blockNum = registrationNotifyEvent.blockNum
        )
    }
}

class SoraEthWithdrawalProofsEvent(
    val accountIdToNotify: String,
    val tokenContractAddress: String,
    val amount: String,
    val relay: String,
    val proofs: List<SoraECDSASignature>,
    val irohaTxHash: String,
    val to: String,
    id: String,
    txTime: Long,
    blockNum: Long,
    txIndex: Int
) : SoraEvent(id = id, txTime = txTime, blockNum = blockNum, txIndex = txIndex) {
    companion object {
        fun map(ethWithdrawalProofsEvent: EthWithdrawalProofsEvent): SoraEthWithdrawalProofsEvent {
            return SoraEthWithdrawalProofsEvent(
                accountIdToNotify = ethWithdrawalProofsEvent.accountIdToNotify,
                tokenContractAddress = ethWithdrawalProofsEvent.tokenContractAddress,
                amount = ethWithdrawalProofsEvent.amount.toPlainString(),
                relay = ethWithdrawalProofsEvent.relay,
                proofs = ethWithdrawalProofsEvent.proofs.map { SoraECDSASignature.map(it) },
                irohaTxHash = ethWithdrawalProofsEvent.irohaTxHash,
                to = ethWithdrawalProofsEvent.to,
                id = ethWithdrawalProofsEvent.id,
                txTime = ethWithdrawalProofsEvent.txTime,
                blockNum = ethWithdrawalProofsEvent.blockNum,
                txIndex = ethWithdrawalProofsEvent.txIndex
            )
        }
    }
}

data class SoraECDSASignature(
    val r: String,
    val s: String,
    val v: String
) {
    companion object {
        fun map(ecdsaSignature: ECDSASignature): SoraECDSASignature {
            return SoraECDSASignature(
                r = ecdsaSignature.r,
                s = ecdsaSignature.s,
                v = ecdsaSignature.v
            )
        }
    }
}

class SoraAckEthWithdrawalProofEvent(
    val proofEventId: String,
    id: String,
    txTime: Long,
    blockNum: Long,
    txIndex: Int
) : SoraEvent(id = id, txTime = txTime, blockNum = blockNum, txIndex = txIndex) {
    companion object {
        fun map(ackEthWithdrawalProofEvent: AckEthWithdrawalProofEvent): SoraAckEthWithdrawalProofEvent {
            return SoraAckEthWithdrawalProofEvent(
                proofEventId = ackEthWithdrawalProofEvent.proofEventId,
                id = ackEthWithdrawalProofEvent.id,
                txTime = ackEthWithdrawalProofEvent.txTime,
                blockNum = ackEthWithdrawalProofEvent.blockNum,
                txIndex = ackEthWithdrawalProofEvent.txIndex
            )
        }
    }
}
