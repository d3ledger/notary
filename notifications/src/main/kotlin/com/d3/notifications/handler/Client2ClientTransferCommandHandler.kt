/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.handler

import com.d3.commons.provider.NotaryClientsProvider
import com.d3.notifications.config.NotificationsConfig
import com.d3.notifications.event.Client2ClientReceiveTransferEvent
import com.d3.notifications.event.Client2ClientSendTransferEvent
import com.d3.notifications.event.TransferFee
import com.d3.notifications.queue.EventsQueue
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Handler that handles client-to-client transfer events
 */
@Component
class Client2ClientTransferCommandHandler(
    private val notificationsConfig: NotificationsConfig,
    private val notaryClientsProvider: NotaryClientsProvider,
    private val eventsQueue: EventsQueue
) : CommandHandler() {
    override fun handle(irohaCommand: IrohaCommand) {
        val transferAsset = irohaCommand.command.transferAsset
        val tx = irohaCommand.tx
        val description: String = transferAsset.description ?: ""
        val transferNotifyReceiveEvent = Client2ClientReceiveTransferEvent(
            accountIdToNotify = transferAsset.destAccountId,
            amount = BigDecimal(transferAsset.amount),
            assetName = transferAsset.assetId,
            description = description,
            from = transferAsset.srcAccountId,
            id = Utils.toHex(Utils.hash(tx)) + "_receive",
            txTime = tx.payload.reducedPayload.createdTime,
            fee = getTransferFee(tx),
            blockNum = irohaCommand.block.blockV1.payload.height,
            txIndex = irohaCommand.getTxIndex()
        )
        val transferNotifySendEvent = Client2ClientSendTransferEvent(
            accountIdToNotify = transferAsset.srcAccountId,
            amount = BigDecimal(transferAsset.amount),
            assetName = transferAsset.assetId,
            description = description,
            to = transferAsset.destAccountId,
            fee = getTransferFee(tx),
            id = Utils.toHex(Utils.hash(tx)) + "_send",
            txTime = tx.payload.reducedPayload.createdTime,
            blockNum = irohaCommand.block.blockV1.payload.height,
            txIndex = irohaCommand.getTxIndex()
        )
        logger.info("Notify transfer receive $transferNotifyReceiveEvent")
        logger.info("Notify transfer send $transferNotifySendEvent")
        eventsQueue.enqueue(transferNotifyReceiveEvent)
        eventsQueue.enqueue(transferNotifySendEvent)
    }

    override fun ableToHandle(irohaCommand: IrohaCommand) = safeCheck {
        if (!irohaCommand.command.hasTransferAsset()) {
            return false
        }
        val transferAsset = irohaCommand.command.transferAsset
        return transferAsset.destAccountId != notificationsConfig.transferBillingAccount
                && notaryClientsProvider.isClient(transferAsset.srcAccountId).get()
                && notaryClientsProvider.isClient(transferAsset.destAccountId).get()
    }

    /**
     * Returns transfer fee
     * @param tx - transaction that is used to find transfer fee
     * @return transfer fee or null if not found
     */
    private fun getTransferFee(
        tx: TransactionOuterClass.Transaction
    ): TransferFee? {
        val feeTransfer = tx.payload.reducedPayload.commandsList.find { command ->
            command.hasTransferAsset()
                    && command.transferAsset.destAccountId == notificationsConfig.transferBillingAccount
                    && notaryClientsProvider.isClient(command.transferAsset.srcAccountId).get()
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
