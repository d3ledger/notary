/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.init

import com.d3.chainadapter.client.ReliableIrohaChainListener
import com.d3.commons.provider.NotaryClientsProvider
import com.d3.commons.registration.FAILED_REGISTRATION_KEY
import com.d3.commons.service.LAST_SUCCESSFUL_WITHDRAWAL_KEY
import com.d3.commons.service.WithdrawalFinalizationDetails
import com.d3.commons.sidechain.iroha.FEE_ROLLBACK_DESCRIPTION
import com.d3.commons.sidechain.iroha.NOTARY_DOMAIN
import com.d3.commons.sidechain.iroha.ROLLBACK_DESCRIPTION
import com.d3.commons.sidechain.iroha.util.CommandWithCreator
import com.d3.commons.sidechain.iroha.util.getSetAccountDetailTransactions
import com.d3.commons.sidechain.iroha.util.getTransferTransactions
import com.d3.commons.util.irohaUnEscape
import com.d3.notifications.config.NotificationsConfig
import com.d3.notifications.event.*
import com.d3.notifications.queue.EventsQueue
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import iroha.protocol.BlockOuterClass
import iroha.protocol.Commands
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.Utils
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
                    { (block, ack) ->
                        try {
                            handleBlock(block)
                        } catch (e: Exception) {
                            logger.error("Cannot handle block $block", e)
                        } finally {
                            ack()
                        }
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
        getSetAccountDetailTransactions(block)
            .forEach { tx ->
                tx.payload.reducedPayload.commandsList.forEach { command ->
                    //TODO omg refactor it
                    val commandWithCreator = CommandWithCreator(command, tx.payload.reducedPayload.creatorAccountId)
                    // Notify withdrawal
                    if (isWithdrawal(commandWithCreator)) {
                        handleWithdrawalEventNotification(command.setAccountDetail, tx)
                    }
                    // Notify Ethereum registration
                    else if (isEthRegistration(commandWithCreator)) {
                        handleEthRegistrationEventNotification(command.setAccountDetail, tx)
                    }
                    // Notify Bitcoin registration
                    else if (isBtcRegistration(commandWithCreator)) {
                        handleBtcRegistrationEventNotification(command.setAccountDetail, tx)
                    }
                    // Notify failed Ethereum registration
                    else if (isFailedEthRegistration(commandWithCreator)) {
                        handleFailedEthRegistrationEventNotification(command.setAccountDetail, tx)
                    }
                    // Notify failed Bitcoin registration
                    else if (isFailedBtcRegistration(commandWithCreator)) {
                        handleFailedBtcRegistrationEventNotification(command.setAccountDetail, tx)
                    }
                }
            }
        //Get transfer commands from block
        getTransferTransactions(block).forEach { tx ->
            tx.payload.reducedPayload.commandsList.forEach { command ->
                val transferAsset = command.transferAsset
                // Notify deposit
                if (isDeposit(transferAsset)) {
                    handleDepositNotification(transferAsset, tx)
                }
                // Notify transfer
                else if (isClientToClientTransfer(transferAsset)) {
                    handleClientToClientEventNotification(
                        transferAsset,
                        tx,
                        getTransferFee(tx)
                    )
                }
                // Notify rollback
                else if (isRollback(transferAsset)) {
                    handleRollbackEventNotification(
                        transferAsset,
                        tx,
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
    private fun getTransferFee(tx: TransactionOuterClass.Transaction): TransferFee? {
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

    // Checks if transfer is client to client
    private fun isClientToClientTransfer(transferAsset: Commands.TransferAsset) =
        safeCheck {
            return transferAsset.destAccountId != notificationsConfig.transferBillingAccount
                    && notaryClientsProvider.isClient(transferAsset.srcAccountId).get()
                    && notaryClientsProvider.isClient(transferAsset.destAccountId).get()
        }

    // Checks if Ethereum registration event
    private fun isEthRegistration(commandWithCreator: CommandWithCreator) =
        isRegistration(commandWithCreator, notificationsConfig.ethRegistrationServiceAccount, ETH_WALLET)

    // Checks if Bitcoin registration event
    private fun isBtcRegistration(commandWithCreator: CommandWithCreator) =
        isRegistration(commandWithCreator, notificationsConfig.btcRegistrationServiceAccount, BTC_WALLET)

    // Checks if failed Ethereum registration event
    private fun isFailedEthRegistration(commandWithCreator: CommandWithCreator) =
        isFailedRegistration(commandWithCreator, notificationsConfig.ethRegistrationServiceAccount)

    // Checks if failed Btc registration event
    private fun isFailedBtcRegistration(commandWithCreator: CommandWithCreator) =
        isFailedRegistration(commandWithCreator, notificationsConfig.btcRegistrationServiceAccount)

    /**
     * Checks if command is a 'failed registration' command
     * @param commandWithCreator - command with its creator
     * @param registrationAccount - account that register clients in sidechains
     * @return true if a command is a 'failed registration' command
     */
    private fun isFailedRegistration(
        commandWithCreator: CommandWithCreator,
        registrationAccount: String
    ) = safeCheck {
        return commandWithCreator.creator == registrationAccount &&
                commandWithCreator.command.setAccountDetail.key == FAILED_REGISTRATION_KEY &&
                notaryClientsProvider.isClient(commandWithCreator.command.setAccountDetail.accountId).get()
    }

    /**
     * Checks if command is a registration command
     * @param commandWithCreator - Iroha command
     * @param registrationAccount - account that register clients in sidechains
     * @param sideChainWalletKey - key of a sidechain wallet
     * @return true if a command is a registration command
     */
    private fun isRegistration(
        commandWithCreator: CommandWithCreator,
        registrationAccount: String,
        sideChainWalletKey: String
    ) =
        safeCheck {
            return commandWithCreator.creator == registrationAccount &&
                    commandWithCreator.command.setAccountDetail.key == sideChainWalletKey && notaryClientsProvider.isClient(
                commandWithCreator.command.setAccountDetail.accountId
            ).get()
        }

    // Checks if withdrawal event
    private fun isWithdrawal(commandWithCreator: CommandWithCreator) =
        safeCheck {
            val setAccountDetail = commandWithCreator.command.setAccountDetail
            val storageAccountId = setAccountDetail.accountId
            return storageAccountId.endsWith("@$NOTARY_DOMAIN") && setAccountDetail.key == LAST_SUCCESSFUL_WITHDRAWAL_KEY && storageAccountId == commandWithCreator.creator
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
    private fun handleDepositNotification(
        transferAsset: Commands.TransferAsset,
        tx: TransactionOuterClass.Transaction
    ) {
        val transferNotifyEvent = TransferNotifyEvent(
            type = TransferEventType.DEPOSIT,
            accountIdToNotify = transferAsset.destAccountId,
            amount = BigDecimal(transferAsset.amount),
            assetName = transferAsset.assetId,
            from = transferAsset.description,
            id = Utils.toHex(Utils.hash(tx)) + "_deposit",
            time = tx.payload.reducedPayload.createdTime
        )
        logger.info("Notify deposit $transferNotifyEvent")
        eventsQueue.enqueue(transferNotifyEvent)
    }

    // Handles Ethereum registration event notification
    private fun handleEthRegistrationEventNotification(
        setAccountDetail: Commands.SetAccountDetail,
        tx: TransactionOuterClass.Transaction
    ) = handleRegistrationEventNotification(setAccountDetail, tx, RegistrationEventSubsystem.ETH)

    // Handles Bitcoin registration event notification
    private fun handleBtcRegistrationEventNotification(
        setAccountDetail: Commands.SetAccountDetail,
        tx: TransactionOuterClass.Transaction
    ) = handleRegistrationEventNotification(setAccountDetail, tx, RegistrationEventSubsystem.BTC)

    /**
     * Handles registration event
     * @param setAccountDetail - command with registration event
     * @param subsystem - registration subsystem(Ethereum, Bitcoin, etc)
     */
    private fun handleRegistrationEventNotification(
        setAccountDetail: Commands.SetAccountDetail,
        tx: TransactionOuterClass.Transaction,
        subsystem: RegistrationEventSubsystem
    ) {
        val registrationNotifyEvent =
            RegistrationNotifyEvent(
                subsystem = subsystem,
                accountId = setAccountDetail.accountId,
                address = setAccountDetail.value,
                id = Utils.toHex(Utils.hash(tx)) + "_registration",
                time = tx.payload.reducedPayload.createdTime
            )
        logger.info("Notify ${subsystem.name} registration $registrationNotifyEvent")
        eventsQueue.enqueue(registrationNotifyEvent)
    }

    // Handles failed Ethereum registration event notification
    private fun handleFailedEthRegistrationEventNotification(
        setAccountDetail: Commands.SetAccountDetail,
        tx: TransactionOuterClass.Transaction
    ) = handleFailedRegistrationEventNotification(setAccountDetail, tx, RegistrationEventSubsystem.ETH)

    // Handles failed Bitcoin registration event notification
    private fun handleFailedBtcRegistrationEventNotification(
        setAccountDetail: Commands.SetAccountDetail,
        tx: TransactionOuterClass.Transaction
    ) = handleFailedRegistrationEventNotification(setAccountDetail, tx, RegistrationEventSubsystem.BTC)

    /**
     * Handles 'failed registration' event
     * @param setAccountDetail - command with registration event
     * @param subsystem - registration subsystem(Ethereum, Bitcoin, etc)
     */
    private fun handleFailedRegistrationEventNotification(
        setAccountDetail: Commands.SetAccountDetail,
        tx: TransactionOuterClass.Transaction,
        subsystem: RegistrationEventSubsystem
    ) {
        val failedRegistrationNotifyEvent =
            FailedRegistrationNotifyEvent(
                subsystem = subsystem,
                accountId = setAccountDetail.accountId,
                id = Utils.toHex(Utils.hash(tx)) + "_failed_registration",
                time = tx.payload.reducedPayload.createdTime
            )
        logger.info("Notify ${subsystem.name} failed registration $failedRegistrationNotifyEvent")
        eventsQueue.enqueue(failedRegistrationNotifyEvent)
    }

    // Handles withdrawal event notification
    private fun handleWithdrawalEventNotification(
        setAccountDetail: Commands.SetAccountDetail,
        tx: TransactionOuterClass.Transaction
    ) {
        val withdrawalFinalizationDetails =
            WithdrawalFinalizationDetails.fromJson(setAccountDetail.value.irohaUnEscape())
        val operationFee = if (withdrawalFinalizationDetails.feeAmount > BigDecimal.ZERO) {
            TransferFee(withdrawalFinalizationDetails.feeAmount, withdrawalFinalizationDetails.feeAssetId)
        } else {
            null
        }
        val transferNotifyEvent = TransferNotifyEvent(
            type = TransferEventType.WITHDRAWAL,
            accountIdToNotify = withdrawalFinalizationDetails.srcAccountId,
            amount = withdrawalFinalizationDetails.withdrawalAmount,
            assetName = withdrawalFinalizationDetails.withdrawalAssetId,
            to = withdrawalFinalizationDetails.destinationAddress,
            fee = operationFee,
            id = Utils.toHex(Utils.hash(tx)) + "_withdrawal",
            time = tx.payload.reducedPayload.createdTime
        )
        logger.info("Notify withdrawal $transferNotifyEvent")
        eventsQueue.enqueue(transferNotifyEvent)
    }

    // Handles transfer event notification
    private fun handleClientToClientEventNotification(
        transferAsset: Commands.TransferAsset,
        tx: TransactionOuterClass.Transaction,
        fee: TransferFee?
    ) {
        val description: String = transferAsset.description ?: ""
        val transferNotifyReceiveEvent = TransferNotifyEvent(
            type = TransferEventType.TRANSFER_RECEIVE,
            accountIdToNotify = transferAsset.destAccountId,
            amount = BigDecimal(transferAsset.amount),
            assetName = transferAsset.assetId,
            description = description,
            from = transferAsset.srcAccountId,
            id = Utils.toHex(Utils.hash(tx)) + "_receive",
            time = tx.payload.reducedPayload.createdTime
        )
        val transferNotifySendEvent = TransferNotifyEvent(
            type = TransferEventType.TRANSFER_SEND,
            accountIdToNotify = transferAsset.srcAccountId,
            amount = BigDecimal(transferAsset.amount),
            assetName = transferAsset.assetId,
            description = description,
            to = transferAsset.destAccountId,
            fee = fee,
            id = Utils.toHex(Utils.hash(tx)) + "_send",
            time = tx.payload.reducedPayload.createdTime
        )
        logger.info("Notify transfer receive $transferNotifyReceiveEvent")
        logger.info("Notify transfer send $transferNotifySendEvent")
        eventsQueue.enqueue(transferNotifyReceiveEvent)
        eventsQueue.enqueue(transferNotifySendEvent)
    }

    // Handles rollback event notification
    private fun handleRollbackEventNotification(
        transferAsset: Commands.TransferAsset,
        tx: TransactionOuterClass.Transaction,
        rollbackFee: TransferFee?
    ) {

        val transferNotifyEvent = TransferNotifyEvent(
            type = TransferEventType.ROLLBACK,
            accountIdToNotify = transferAsset.destAccountId,
            amount = BigDecimal(transferAsset.amount),
            assetName = transferAsset.assetId,
            fee = rollbackFee,
            id = Utils.toHex(Utils.hash(tx)) + "_rollback",
            time = tx.payload.reducedPayload.createdTime
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

    /**
     * Logger
     */
    companion object : KLogging()
}
