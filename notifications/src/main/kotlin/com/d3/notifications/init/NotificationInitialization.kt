/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.init

import com.d3.commons.service.LAST_SUCCESSFUL_WITHDRAWAL_KEY
import com.d3.commons.service.WithdrawalFinalizationDetails
import com.d3.commons.sidechain.iroha.CLIENT_DOMAIN
import com.d3.commons.sidechain.iroha.IrohaChainListener
import com.d3.commons.sidechain.iroha.NOTARY_DOMAIN
import com.d3.commons.sidechain.iroha.ROLLBACK_DESCRIPTION
import com.d3.commons.sidechain.iroha.util.getSetDetailCommands
import com.d3.commons.sidechain.iroha.util.getTransferTransactions
import com.d3.commons.util.createPrettySingleThreadPool
import com.d3.commons.util.irohaUnEscape
import com.d3.notifications.NOTIFICATIONS_SERVICE_NAME
import com.d3.notifications.config.NotificationsConfig
import com.d3.notifications.service.NotificationService
import com.d3.notifications.service.TransferNotifyEvent
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import io.reactivex.schedulers.Schedulers
import iroha.protocol.Commands
import iroha.protocol.TransactionOuterClass
import mu.KLogging
import org.springframework.stereotype.Component
import java.math.BigDecimal

//TODO refactor handlers
/**
 * Notifications initialization service
 */
@Component
class NotificationInitialization(
    private val notificationsConfig: NotificationsConfig,
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
                        getSetDetailCommands(block).map { it.setAccountDetail }.forEach { setDetailCommand ->
                            // Notify withdrawal
                            if (isWithdrawal(setDetailCommand)) {
                                handleWithdrawalEventNotification(setDetailCommand)
                            }
                        }
                        //Get transfer commands from block
                        getTransferTransactions(block).forEach { tx ->
                            tx.payload.reducedPayload.commandsList.forEach { command ->
                                val transferAsset = command.transferAsset
                                // Notify deposit
                                if (isDeposit(transferAsset)) {
                                    handleDepositNotification(transferAsset)
                                }
                                // Notify transfer
                                else if (isClientToClientTransfer(transferAsset)) {
                                    handleClientToClientEventNotification(transferAsset, getTransferFee(tx))
                                }
                                // Notify rollback
                                else if (isRollback(transferAsset)) {
                                    handleRollbackEventNotification(transferAsset)
                                }
                            }
                        }
                    }, { ex ->
                        logger.error("Error on Iroha subscribe", ex)
                        onIrohaChainFailure()
                    })
        }
    }

    /**
     * Returns transfer fee
     * @param tx - transaction that is used to find transfer fee
     * @return transfer fee or null if not found
     */
    private fun getTransferFee(tx: TransactionOuterClass.Transaction): OperationFee? {
        val feeTransfer = tx.payload.reducedPayload.commandsList.find { command ->
            command.hasTransferAsset() && command.transferAsset.srcAccountId.endsWith(
                "@$CLIENT_DOMAIN"
            ) && command.transferAsset.destAccountId == notificationsConfig.transferBillingAccount
        }
        return if (feeTransfer == null) {
            null
        } else {
            OperationFee(BigDecimal(feeTransfer.transferAsset.amount), feeTransfer.transferAsset.assetId)
        }
    }

    // Checks if transfer is client to client
    private fun isClientToClientTransfer(transferAsset: Commands.TransferAsset) = transferAsset.srcAccountId.endsWith(
        "@$CLIENT_DOMAIN"
    ) && (transferAsset.destAccountId.endsWith("@$CLIENT_DOMAIN") &&
            transferAsset.destAccountId != notificationsConfig.transferBillingAccount)

    // Checks if withdrawal event
    private fun isWithdrawal(setAccountDetail: Commands.SetAccountDetail) =
        setAccountDetail.accountId.endsWith("@$NOTARY_DOMAIN") && setAccountDetail.key == LAST_SUCCESSFUL_WITHDRAWAL_KEY

    // Checks if deposit event
    private fun isDeposit(transferAsset: Commands.TransferAsset): Boolean {
        val depositSign =
            transferAsset.srcAccountId.endsWith("@$NOTARY_DOMAIN")
                    && (transferAsset.destAccountId.endsWith("@$CLIENT_DOMAIN") &&
                    transferAsset.destAccountId != notificationsConfig.transferBillingAccount &&
                    transferAsset.destAccountId != notificationsConfig.withdrawalBillingAccount)
        return depositSign && !isRollbackSign(transferAsset)
    }

    // Checks if rollback event
    private fun isRollback(transferAsset: Commands.TransferAsset): Boolean {
        val depositSign = transferAsset.srcAccountId.endsWith("@$NOTARY_DOMAIN")
                && (transferAsset.destAccountId.endsWith("@$CLIENT_DOMAIN") &&
                transferAsset.destAccountId != notificationsConfig.transferBillingAccount &&
                transferAsset.destAccountId != notificationsConfig.withdrawalBillingAccount)
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
            description = "",
            from = transferAsset.description
        )
        logger.info { "Notify deposit $transferNotifyEvent" }
        notificationServices.forEach {
            it.notifyDeposit(
                transferNotifyEvent
            ).failure { ex -> logger.error("Cannot notify deposit: $transferNotifyEvent", ex) }
        }
    }

    // Handles withdrawal event notification
    private fun handleWithdrawalEventNotification(setAccountDetail: Commands.SetAccountDetail) {
        val withdrawalFinalizationDetails =
            WithdrawalFinalizationDetails.fromJson(setAccountDetail.value.irohaUnEscape())
        val withdrawalDescription = if (withdrawalFinalizationDetails.feeAmount > BigDecimal.ZERO) {
            "Fee is ${withdrawalFinalizationDetails.feeAmount} ${withdrawalFinalizationDetails.feeAssetId}"
        } else {
            ""
        }
        val transferNotifyEvent = TransferNotifyEvent(
            withdrawalFinalizationDetails.srcAccountId,
            withdrawalFinalizationDetails.withdrawalAmount,
            withdrawalFinalizationDetails.withdrawalAssetId,
            withdrawalDescription,
            to = withdrawalFinalizationDetails.destinationAddress
        )
        logger.info { "Notify withdrawal $transferNotifyEvent" }
        notificationServices.forEach {
            it.notifyWithdrawal(
                transferNotifyEvent
            ).failure { ex -> logger.error("Cannot notify withdrawal: $transferNotifyEvent", ex) }
        }
    }

    // Handles transfer event notification
    private fun handleClientToClientEventNotification(
        transferAsset: Commands.TransferAsset,
        fee: OperationFee?
    ) {
        val transferNotifyReceiveEvent = TransferNotifyEvent(
            transferAsset.destAccountId,
            BigDecimal(transferAsset.amount),
            transferAsset.assetId,
            description = "",
            from = transferAsset.srcAccountId
        )
        val transferDescription = if (fee == null) {
            ""
        } else {
            "Fee is ${fee.feeAmount} ${fee.feeAssetId}."
        }
        val transferNotifySendEvent = TransferNotifyEvent(
            transferAsset.srcAccountId,
            BigDecimal(transferAsset.amount),
            transferAsset.assetId,
            transferDescription,
            to = transferAsset.destAccountId
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

    private data class OperationFee(val feeAmount: BigDecimal, val feeAssetId: String)

    /**
     * Logger
     */
    companion object : KLogging()
}
