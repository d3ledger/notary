/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.handler

import com.d3.commons.service.LAST_SUCCESSFUL_WITHDRAWAL_KEY
import com.d3.commons.service.WithdrawalFinalizationDetails
import com.d3.commons.sidechain.iroha.NOTARY_DOMAIN
import com.d3.commons.util.irohaUnEscape
import com.d3.notifications.event.TransferFee
import com.d3.notifications.event.WithdrawalTransferEvent
import com.d3.notifications.queue.EventsQueue
import jp.co.soramitsu.iroha.java.Utils
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Handler that handles withdrawal commands
 */
@Component
class WithdrawalCommandHandler(private val eventsQueue: EventsQueue) : CommandHandler() {

    // Handles withdrawal event notification
    override fun handle(commandWithTx: CommandWithTx) {
        val withdrawalFinalizationDetails =
            WithdrawalFinalizationDetails.fromJson(commandWithTx.command.setAccountDetail.value.irohaUnEscape())
        val operationFee = if (withdrawalFinalizationDetails.feeAmount > BigDecimal.ZERO) {
            TransferFee(withdrawalFinalizationDetails.feeAmount, withdrawalFinalizationDetails.feeAssetId)
        } else {
            null
        }
        val transferNotifyEvent = WithdrawalTransferEvent(
            accountIdToNotify = withdrawalFinalizationDetails.srcAccountId,
            amount = withdrawalFinalizationDetails.withdrawalAmount,
            assetName = withdrawalFinalizationDetails.withdrawalAssetId,
            to = withdrawalFinalizationDetails.destinationAddress,
            fee = operationFee,
            id = Utils.toHex(Utils.hash(commandWithTx.tx)) + "_withdrawal",
            time = commandWithTx.tx.payload.reducedPayload.createdTime,
            sideChainFee = withdrawalFinalizationDetails.sideChainFee
        )
        logger.info("Notify withdrawal $transferNotifyEvent")
        eventsQueue.enqueue(transferNotifyEvent)
    }

    // Checks if withdrawal event
    override fun ableToHandle(commandWithTx: CommandWithTx) = safeCheck {
        if (!commandWithTx.command.hasSetAccountDetail()) {
            return false
        }
        val setAccountDetail = commandWithTx.command.setAccountDetail
        val storageAccountId = setAccountDetail.accountId
        return storageAccountId.endsWith("@$NOTARY_DOMAIN") &&
                setAccountDetail.key == LAST_SUCCESSFUL_WITHDRAWAL_KEY &&
                storageAccountId == commandWithTx.tx.getCreator()
    }

}
