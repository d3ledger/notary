/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.init

import com.d3.commons.sidechain.iroha.*
import com.d3.commons.sidechain.iroha.util.getTransferCommands
import com.d3.commons.util.createPrettySingleThreadPool
import com.d3.notifications.NOTIFICATIONS_SERVICE_NAME
import com.d3.notifications.service.NotificationService
import com.d3.notifications.service.TransferNotifyEvent
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import io.reactivex.schedulers.Schedulers
import iroha.protocol.Commands
import mu.KLogging
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Notifications initialization service
 */
@Component
class NotificationInitialization(
    private val irohaChainListener: IrohaChainListener,
    private val notificationServices: List<NotificationService>
) {

    /**
     * Initiates notification service
     * @param onIrohaChainFailure - function that will be called in case of Iroha failure. Does nothing by default.
     */
    fun init(onIrohaChainFailure: () -> Unit = {}) {
        irohaChainListener.getBlockObservable().map { irohaObservable ->
            irohaObservable
                .subscribeOn(
                    Schedulers.from(
                        createPrettySingleThreadPool(
                            NOTIFICATIONS_SERVICE_NAME, "iroha-chain-listener"
                        )
                    )
                )
                .subscribe(
                    { block ->
                        //Get transfer commands from block
                        getTransferCommands(block).forEach { command ->
                            val transferAsset = command.transferAsset
                            // Notify deposit
                            if (isDeposit(transferAsset)) {
                                handleDepositNotification(transferAsset)
                            }
                            // Notify withdrawal
                            else if (isWithdrawal(transferAsset)) {
                                handleWithdrawalEventNotification(transferAsset)
                            }
                            // Notify transfer
                            else if (isClientToClientTransfer(transferAsset)) {
                                handleClientToClientEventNotification(transferAsset)
                            }
                            // Notify rollback
                            else if (isRollback(transferAsset)) {
                                handleRollbackEventNotification(transferAsset)
                            }
                        }
                    }, { ex ->
                        logger.error("Error on Iroha subscribe", ex)
                        onIrohaChainFailure()
                    })
        }
    }

    // Checks if transfer is client to client
    private fun isClientToClientTransfer(transferAsset: Commands.TransferAsset) = transferAsset.srcAccountId.endsWith(
        "@$CLIENT_DOMAIN"
    ) && transferAsset.destAccountId.endsWith("@$CLIENT_DOMAIN")

    // Checks if withdrawal event
    private fun isWithdrawal(transferAsset: Commands.TransferAsset) =
        transferAsset.destAccountId.endsWith("@$NOTARY_DOMAIN") && transferAsset.description != FEE_DESCRIPTION

    // Checks if deposit event
    private fun isDeposit(transferAsset: Commands.TransferAsset): Boolean {
        val depositSign = transferAsset.srcAccountId.endsWith("@$NOTARY_DOMAIN")
        return depositSign && !isRollbackSign(transferAsset)
    }

    // Checks if rollback event
    private fun isRollback(transferAsset: Commands.TransferAsset): Boolean {
        val depositSign = transferAsset.srcAccountId.endsWith("@$NOTARY_DOMAIN")
        return depositSign && isRollbackSign(transferAsset)
    }

    // Check if command contains any signs of rollback
    private fun isRollbackSign(transferAsset: Commands.TransferAsset): Boolean {
        return if (transferAsset.description != null) {
            transferAsset.description.startsWith(ROLLBACK_DESCRIPTION)
        } else {
            false
        }
    }

    // Handles deposit event notification
    private fun handleDepositNotification(transferAsset: Commands.TransferAsset) {
        val transferNotifyEvent = TransferNotifyEvent(
            transferAsset.destAccountId,
            BigDecimal(transferAsset.amount),
            transferAsset.assetId,
            transferAsset.description
        )
        logger.info { "Notify deposit $transferNotifyEvent" }
        notificationServices.forEach {
            it.notifyDeposit(
                transferNotifyEvent
            ).failure { ex -> logger.error("Cannot notify deposit: $transferNotifyEvent", ex) }
        }
    }

    // Handles withdrawal event notification
    private fun handleWithdrawalEventNotification(transferAsset: Commands.TransferAsset) {
        val transferNotifyEvent = TransferNotifyEvent(
            transferAsset.srcAccountId,
            BigDecimal(transferAsset.amount),
            transferAsset.assetId,
            transferAsset.description
        )
        logger.info { "Notify withdrawal $transferNotifyEvent" }
        notificationServices.forEach {
            it.notifyWithdrawal(
                transferNotifyEvent
            ).failure { ex -> logger.error("Cannot notify withdrawal: $transferNotifyEvent", ex) }
        }
    }

    // Handles transfer event notification
    private fun handleClientToClientEventNotification(transferAsset: Commands.TransferAsset) {
        val transferNotifyReceiveEvent = TransferNotifyEvent(
            transferAsset.destAccountId,
            BigDecimal(transferAsset.amount),
            transferAsset.assetId,
            transferAsset.description
        )
        val transferNotifySendEvent = TransferNotifyEvent(
            transferAsset.srcAccountId,
            BigDecimal(transferAsset.amount),
            transferAsset.assetId,
            transferAsset.description
        )
        logger.info { "Notify transfer receive $transferNotifyReceiveEvent" }
        logger.info { "Notify transfer send $transferNotifySendEvent" }
        notificationServices.forEach {
            it.notifySendToClient(
                transferNotifySendEvent
            ).failure { ex -> logger.error("Cannot notify transfer: $transferNotifySendEvent", ex) }

            it.notifyReceiveFromClient(
                transferNotifyReceiveEvent
            ).failure { ex -> logger.error("Cannot notify transfer: $transferNotifyReceiveEvent", ex) }
        }
    }

    // Handles rollback event notification
    private fun handleRollbackEventNotification(transferAsset: Commands.TransferAsset) {
        val transferNotifyEvent = TransferNotifyEvent(
            transferAsset.destAccountId,
            BigDecimal(transferAsset.amount),
            transferAsset.assetId,
            transferAsset.description
        )
        logger.info { "Notify rollback $transferNotifyEvent" }
        notificationServices.forEach {
            it.notifyRollback(
                transferNotifyEvent
            ).failure { ex -> logger.error("Cannot notify rollback: $transferNotifyEvent", ex) }
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
