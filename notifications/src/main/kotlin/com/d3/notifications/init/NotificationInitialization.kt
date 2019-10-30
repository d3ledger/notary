/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.init

import com.d3.chainadapter.client.ReliableIrohaChainListener
import com.d3.commons.provider.NotaryClientsProvider
import com.d3.commons.service.LAST_SUCCESSFUL_WITHDRAWAL_KEY
import com.d3.commons.service.WithdrawalFinalizationDetails
import com.d3.commons.sidechain.iroha.FEE_ROLLBACK_DESCRIPTION
import com.d3.commons.sidechain.iroha.NOTARY_DOMAIN
import com.d3.commons.sidechain.iroha.ROLLBACK_DESCRIPTION
import com.d3.commons.sidechain.iroha.util.getSetDetailCommands
import com.d3.commons.sidechain.iroha.util.getTransferTransactions
import com.d3.commons.util.irohaUnEscape
import com.d3.notifications.config.NotificationsConfig
import com.d3.notifications.service.NotificationService
import com.d3.notifications.service.TransferNotifyEvent
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import iroha.protocol.BlockOuterClass
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
    private val notaryClientsProvider: NotaryClientsProvider,
    private val notificationsConfig: NotificationsConfig,
    private val irohaChainListener: ReliableIrohaChainListener,
    private val notificationServices: List<NotificationService>
) {

    /**
     * Initiates notification service
     * @param onIrohaChainFailure - function that will be called in case of Iroha failure. Does nothing by default.
     */
    fun init(onIrohaChainFailure: () -> Unit = {}): Result<Unit, Exception> {
        return irohaChainListener.getBlockObservable().map { irohaObservable ->
            irohaObservable
                .subscribe(
                    { (block, ack) ->
                        safeAck({ handleBlock(block) }, ack)
                    }, { ex ->
                        logger.error("Error on Iroha subscribe", ex)
                        onIrohaChainFailure()
                    })
        }.flatMap { irohaChainListener.listen() }
    }

    /**
     * Handles given block
     * @param block - block to handle
     */
    private fun handleBlock(block: BlockOuterClass.Block) {
        getSetDetailCommands(block).map { it.setAccountDetail }
            .forEach { setDetailCommand ->
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
                    handleClientToClientEventNotification(
                        transferAsset,
                        getTransferFee(tx)
                    )
                }
                // Notify rollback
                else if (isRollback(transferAsset)) {
                    handleRollbackEventNotification(
                        transferAsset,
                        getWithdrawalRollbackFee(tx)
                    )
                }
            }
        }
    }

    /**
     * Returns transfer fee
     * @param tx - transaction that is used to find transfer fee
     * @return transfer fee or null if not found
     */
    private fun getTransferFee(tx: TransactionOuterClass.Transaction): OperationFee? {
        val feeTransfer = tx.payload.reducedPayload.commandsList.find { command ->
            command.hasTransferAsset()
                    && command.transferAsset.destAccountId == notificationsConfig.transferBillingAccount
                    && notaryClientsProvider.isClient(command.transferAsset.srcAccountId).get()
        }
        return if (feeTransfer == null) {
            null
        } else {
            OperationFee(
                BigDecimal(feeTransfer.transferAsset.amount),
                feeTransfer.transferAsset.assetId
            )
        }
    }

    /**
     * Returns withdrawal rollback fee
     * @param tx - transaction that is used to find withdrawal rollback fee
     * @return withdrawal rollback or null if not found
     */
    private fun getWithdrawalRollbackFee(tx: TransactionOuterClass.Transaction): OperationFee? {
        val feeTransfer = tx.payload.reducedPayload.commandsList.find { command ->
            command.hasTransferAsset()
                    && command.transferAsset.srcAccountId.endsWith("@$NOTARY_DOMAIN")
                    && command.transferAsset.description == FEE_ROLLBACK_DESCRIPTION
                    && notaryClientsProvider.isClient(command.transferAsset.destAccountId).get()
        }
        return if (feeTransfer == null) {
            null
        } else {
            OperationFee(
                BigDecimal(feeTransfer.transferAsset.amount),
                feeTransfer.transferAsset.assetId
            )
        }
    }

    // Checks if transfer is client to client
    private fun isClientToClientTransfer(transferAsset: Commands.TransferAsset) =
        safeCheck {
            return transferAsset.destAccountId != notificationsConfig.transferBillingAccount
                    && notaryClientsProvider.isClient(transferAsset.srcAccountId).get()
                    && notaryClientsProvider.isClient(transferAsset.destAccountId).get()
        }

    // Checks if withdrawal event
    private fun isWithdrawal(setAccountDetail: Commands.SetAccountDetail) =
        safeCheck {
            return setAccountDetail.accountId.endsWith("@$NOTARY_DOMAIN") && setAccountDetail.key == LAST_SUCCESSFUL_WITHDRAWAL_KEY
        }

    // Checks if deposit event
    private fun isDeposit(transferAsset: Commands.TransferAsset) =
        safeCheck {
            val depositSign =
                transferAsset.srcAccountId.endsWith("@$NOTARY_DOMAIN")
                        && transferAsset.destAccountId != notificationsConfig.transferBillingAccount
                        && transferAsset.destAccountId != notificationsConfig.withdrawalBillingAccount
                        && notaryClientsProvider.isClient(transferAsset.destAccountId).get()
            return depositSign && !isRollbackSign(transferAsset) && transferAsset.description != FEE_ROLLBACK_DESCRIPTION
        }


    // Checks if rollback event
    private fun isRollback(transferAsset: Commands.TransferAsset) =
        safeCheck {
            val depositSign = transferAsset.srcAccountId.endsWith("@$NOTARY_DOMAIN")
                    && transferAsset.destAccountId != notificationsConfig.transferBillingAccount
                    && transferAsset.destAccountId != notificationsConfig.withdrawalBillingAccount
                    && notaryClientsProvider.isClient(transferAsset.destAccountId).get()
            return depositSign && isRollbackSign(transferAsset)
        }

    // Check if command contains any signs of rollback
    private fun isRollbackSign(transferAsset: Commands.TransferAsset) =
        safeCheck {
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
        logger.info("Notify deposit $transferNotifyEvent")
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
            ).failure { ex ->
                logger.error(
                    "Cannot notify transfer: $transferNotifyReceiveEvent",
                    ex
                )
            }
        }
    }

    // Handles rollback event notification
    private fun handleRollbackEventNotification(
        transferAsset: Commands.TransferAsset,
        rollbackFee: OperationFee?
    ) {
        val rollbackDescription = if (rollbackFee == null) {
            ""
        } else {
            "Fee ${rollbackFee.feeAmount} ${rollbackFee.feeAssetId} is rolled back as well."
        }
        val transferNotifyEvent = TransferNotifyEvent(
            transferAsset.destAccountId,
            BigDecimal(transferAsset.amount),
            transferAsset.assetId,
            rollbackDescription
        )
        logger.info { "Notify rollback $transferNotifyEvent" }
        notificationServices.forEach {
            it.notifyRollback(
                transferNotifyEvent
            ).failure { ex -> logger.error("Cannot notify rollback: $transferNotifyEvent", ex) }
        }
    }

    /**
     * Executes check safely
     */
    private inline fun safeCheck(check: () -> Boolean): Boolean {
        return try {
            check()
        } catch (e: Exception) {
            logger.error("Cannot check", e)
            false
        }
    }

    /**
     * Executes provided logic safely
     * @param logic - logic to execute
     * @param ack - acknowledgment function to call
     */
    private fun safeAck(logic: () -> Unit, ack: () -> Unit) {
        try {
            logic()
        } catch (e: Exception) {
            logger.error("Cannot execute", e)
        } finally {
            ack()
        }
    }

    private data class OperationFee(val feeAmount: BigDecimal, val feeAssetId: String)

    /**
     * Logger
     */
    companion object : KLogging()
}
