/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.handler

import com.d3.commons.provider.NotaryClientsProvider
import com.d3.notifications.config.NotificationsConfig
import com.d3.notifications.event.DepositTransferEvent
import com.d3.notifications.queue.EventsQueue
import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Handler that handles deposit events
 */
@Component
class DepositCommandHandler(
    private val notificationsConfig: NotificationsConfig,
    private val notaryClientsProvider: NotaryClientsProvider,
    private val eventsQueue: EventsQueue
) : CommandHandler() {
    override fun handle(irohaCommand: IrohaCommand) {
        val transferAsset = irohaCommand.command.transferAsset
        val transferNotifyEvent = DepositTransferEvent(
            accountIdToNotify = transferAsset.destAccountId,
            amount = BigDecimal(transferAsset.amount),
            assetName = transferAsset.assetId,
            from = transferAsset.description,
            id = Utils.toHex(Utils.hash(irohaCommand.tx)) + "_deposit",
            txTime = irohaCommand.tx.payload.reducedPayload.createdTime,
            blockNum = irohaCommand.block.blockV1.payload.height,
            txIndex = irohaCommand.getTxIndex()
        )
        logger.info("Notify deposit $transferNotifyEvent")
        eventsQueue.enqueue(transferNotifyEvent)
    }

    override fun ableToHandle(irohaCommand: IrohaCommand) = safeCheck {
        if (!irohaCommand.command.hasTransferAsset()) {
            return false
        }
        val transferAsset = irohaCommand.command.transferAsset
        return transferAsset.srcAccountId == notificationsConfig.ethDepositAccount &&
                transferAsset.destAccountId != notificationsConfig.transferBillingAccount &&
                transferAsset.destAccountId != notificationsConfig.withdrawalBillingAccount &&
                notaryClientsProvider.isClient(transferAsset.destAccountId).get()
    }

    companion object : KLogging()
}
