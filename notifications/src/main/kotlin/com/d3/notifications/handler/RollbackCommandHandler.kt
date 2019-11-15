/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.handler

import com.d3.commons.provider.NotaryClientsProvider
import com.d3.commons.sidechain.iroha.FEE_ROLLBACK_DESCRIPTION
import com.d3.commons.sidechain.iroha.NOTARY_DOMAIN
import com.d3.commons.sidechain.iroha.ROLLBACK_DESCRIPTION
import com.d3.notifications.config.NotificationsConfig
import com.d3.notifications.event.RollbackTransferEvent
import com.d3.notifications.event.TransferFee
import com.d3.notifications.queue.EventsQueue
import iroha.protocol.Commands
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Handler that handles rollback events
 */
@Component
class RollbackCommandHandler(
    private val notificationsConfig: NotificationsConfig,
    private val notaryClientsProvider: NotaryClientsProvider,
    private val eventsQueue: EventsQueue
) : CommandHandler() {
    override fun handle(commandWithTx: CommandWithTx) {
        val transferAsset = commandWithTx.command.transferAsset
        val transferNotifyEvent = RollbackTransferEvent(
            accountIdToNotify = transferAsset.destAccountId,
            amount = BigDecimal(transferAsset.amount),
            assetName = transferAsset.assetId,
            fee = getWithdrawalRollbackFee(commandWithTx.tx),
            id = Utils.toHex(Utils.hash(commandWithTx.tx)) + "_rollback",
            time = commandWithTx.tx.payload.reducedPayload.createdTime
        )
        logger.info("Notify rollback $transferNotifyEvent")
        eventsQueue.enqueue(transferNotifyEvent)
    }

    override fun ableToHandle(commandWithTx: CommandWithTx) = safeCheck {
        if (!commandWithTx.command.hasTransferAsset()) {
            return false
        }
        val transferAsset = commandWithTx.command.transferAsset
        val depositSign =
            (transferAsset.srcAccountId == notificationsConfig.btcWithdrawalAccount || transferAsset.srcAccountId == notificationsConfig.ethWithdrawalAccount)
                    && transferAsset.destAccountId != notificationsConfig.transferBillingAccount
                    && transferAsset.destAccountId != notificationsConfig.withdrawalBillingAccount
                    && notaryClientsProvider.isClient(transferAsset.destAccountId).get()
        return depositSign && isRollbackSign(transferAsset)
    }

    /**
     * Returns withdrawal rollback fee
     * @param tx - transaction that is used to find withdrawal rollback fee
     * @return withdrawal rollback or null if not found
     */
    private fun getWithdrawalRollbackFee(tx: TransactionOuterClass.Transaction): TransferFee? {
        val feeTransfer = tx.payload.reducedPayload.commandsList.find { command ->
            command.hasTransferAsset()
                    && command.transferAsset.srcAccountId.endsWith("@$NOTARY_DOMAIN")
                    && command.transferAsset.description == FEE_ROLLBACK_DESCRIPTION
                    && notaryClientsProvider.isClient(command.transferAsset.destAccountId).get()
        }
        return if (feeTransfer == null) {
            null
        } else {
            TransferFee(
                BigDecimal(feeTransfer.transferAsset.amount),
                feeTransfer.transferAsset.assetId
            )
        }
    }

    companion object : KLogging()
}

// Check if command contains any signs of rollback
fun isRollbackSign(transferAsset: Commands.TransferAsset) =
    safeCheck {
        return if (transferAsset.description != null) {
            transferAsset.description.startsWith(ROLLBACK_DESCRIPTION)
        } else {
            false
        }
    }
