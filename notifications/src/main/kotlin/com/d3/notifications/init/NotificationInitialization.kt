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
import com.d3.commons.sidechain.iroha.util.CommandWithCreator
import com.d3.commons.sidechain.iroha.util.getSetDetailCommandsWithCreator
import com.d3.commons.sidechain.iroha.util.getTransferTransactions
import com.d3.commons.util.irohaUnEscape
import com.d3.notifications.config.NotificationsConfig
import com.d3.notifications.event.RegistrationEventSubsystem
import com.d3.notifications.event.RegistrationNotifyEvent
import com.d3.notifications.event.TransferEventType
import com.d3.notifications.event.TransferNotifyEvent
import com.d3.notifications.queue.EventsQueue
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import iroha.protocol.BlockOuterClass
import iroha.protocol.Commands
import iroha.protocol.TransactionOuterClass
import mu.KLogging
import org.springframework.stereotype.Component
import java.math.BigDecimal

const val ETH_WALLET = "ethereum_wallet"
const val BTC_WALLET = "bitcoin"

//TODO refactor handlers
/**
 * Notifications initialization service
 */
@Component
class NotificationInitialization(
    private val notaryClientsProvider: NotaryClientsProvider,
    private val notificationsConfig: NotificationsConfig,
    private val irohaChainListener: ReliableIrohaChainListener,
    private val eventsQueue: EventsQueue
) {

    /**
     * Initiates notification service
     * @param onIrohaChainFailure - function that will be called in case of Iroha failure. Does nothing by default.
     */
    fun init(onIrohaChainFailure: () -> Unit = {}): Result<Unit, Exception> {
        return irohaChainListener.getBlockObservable().map { irohaObservable ->
            irohaObservable
                .subscribe(
                    { (block, _) ->
                        handleBlock(block)
                    }, { ex ->
                        logger.error("Error on Iroha subscribe", ex)
                        onIrohaChainFailure()
                    })
        }.flatMap { irohaChainListener.listen() }.map { eventsQueue.listen() }
    }

    /**
     * Handles given block
     * @param block - block to handle
     */
    private fun handleBlock(block: BlockOuterClass.Block) {
        getSetDetailCommandsWithCreator(block)
            .forEach { commandWithCreator ->
                val command = commandWithCreator.command.setAccountDetail
                // Notify withdrawal
                if (isWithdrawal(command)) {
                    handleWithdrawalEventNotification(command)
                }
                // Notify Ethereum registration
                else if (isEthRegistration(commandWithCreator)) {
                    handleEthRegistrationEventNotification(command)
                }
                // Notify Bitcoin registration
                else if (isBtcRegistration(commandWithCreator)) {
                    handleBtcRegistrationEventNotification(command)
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

    // Checks if Ethereum registration event
    private fun isEthRegistration(commandWithCreator: CommandWithCreator) =
        safeCheck {
            return commandWithCreator.creator == notificationsConfig.ethRegistrationServiceAccount &&
                    commandWithCreator.command.setAccountDetail.key == ETH_WALLET &&
                    notaryClientsProvider.isClient(commandWithCreator.command.setAccountDetail.accountId).get()
        }

    // Checks if Bitcoin registration event
    private fun isBtcRegistration(commandWithCreator: CommandWithCreator) =
        safeCheck {
            return commandWithCreator.creator == notificationsConfig.btcRegistrationServiceAccount &&
                    commandWithCreator.command.setAccountDetail.key == BTC_WALLET &&
                    notaryClientsProvider.isClient(commandWithCreator.command.setAccountDetail.accountId).get()
        }

    // Checks if withdrawal event
    private fun isWithdrawal(setAccountDetail: Commands.SetAccountDetail) =
        safeCheck {
            return setAccountDetail.accountId.endsWith("@$NOTARY_DOMAIN") && setAccountDetail.key == LAST_SUCCESSFUL_WITHDRAWAL_KEY
        }

    // Checks if deposit event
    private fun isDeposit(transferAsset: Commands.TransferAsset) =
        safeCheck {
            //TODO be more precise
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
            //TODO be more precise
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
            TransferEventType.DEPOSIT,
            transferAsset.destAccountId,
            BigDecimal(transferAsset.amount),
            transferAsset.assetId,
            description = "",
            from = transferAsset.description
        )
        logger.info("Notify deposit $transferNotifyEvent")
        eventsQueue.enqueue(transferNotifyEvent)
    }

    // Handles Ethereum registration event notification
    private fun handleEthRegistrationEventNotification(setAccountDetail: Commands.SetAccountDetail) {
        val registrationNotifyEvent =
            RegistrationNotifyEvent(RegistrationEventSubsystem.ETH, setAccountDetail.accountId, setAccountDetail.value)
        logger.info("Notify ETH registration $registrationNotifyEvent")
        eventsQueue.enqueue(registrationNotifyEvent)
    }

    // Handles Bitcoin registration event notification
    private fun handleBtcRegistrationEventNotification(setAccountDetail: Commands.SetAccountDetail) {
        val registrationNotifyEvent =
            RegistrationNotifyEvent(RegistrationEventSubsystem.BTC, setAccountDetail.accountId, setAccountDetail.value)
        logger.info("Notify BTC registration $registrationNotifyEvent")
        eventsQueue.enqueue(registrationNotifyEvent)
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
            TransferEventType.WITHDRAWAL,
            withdrawalFinalizationDetails.srcAccountId,
            withdrawalFinalizationDetails.withdrawalAmount,
            withdrawalFinalizationDetails.withdrawalAssetId,
            withdrawalDescription,
            to = withdrawalFinalizationDetails.destinationAddress
        )
        logger.info("Notify withdrawal $transferNotifyEvent")
        eventsQueue.enqueue(transferNotifyEvent)
    }

    // Handles transfer event notification
    private fun handleClientToClientEventNotification(
        transferAsset: Commands.TransferAsset,
        fee: OperationFee?
    ) {
        val transferNotifyReceiveEvent = TransferNotifyEvent(
            TransferEventType.TRANSFER_RECEIVE,
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
            TransferEventType.TRANSFER_SEND,
            transferAsset.srcAccountId,
            BigDecimal(transferAsset.amount),
            transferAsset.assetId,
            transferDescription,
            to = transferAsset.destAccountId
        )
        logger.info("Notify transfer receive $transferNotifyReceiveEvent")
        logger.info("Notify transfer send $transferNotifySendEvent")
        eventsQueue.enqueue(transferNotifyReceiveEvent)
        eventsQueue.enqueue(transferNotifySendEvent)
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
            TransferEventType.ROLLBACK,
            transferAsset.destAccountId,
            BigDecimal(transferAsset.amount),
            transferAsset.assetId,
            rollbackDescription
        )
        logger.info("Notify rollback $transferNotifyEvent")
        eventsQueue.enqueue(transferNotifyEvent)
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

    private data class OperationFee(val feeAmount: BigDecimal, val feeAssetId: String)

    /**
     * Logger
     */
    companion object : KLogging()
}
