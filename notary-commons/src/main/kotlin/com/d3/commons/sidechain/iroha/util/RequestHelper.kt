/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.iroha.util

import iroha.protocol.BlockOuterClass
import iroha.protocol.Commands
import iroha.protocol.QryResponses
import iroha.protocol.TransactionOuterClass

/**
 * Returns pretty formatted error message. May be used in exceptions.
 * @param errorResponse - error response that is used to created pretty error message
 */
fun getErrorMessage(errorResponse: QryResponses.ErrorResponse) =
    "Error code ${errorResponse.errorCode} reason ${errorResponse.reason} ${errorResponse.message}"

/**
 * Return all "set account detail" commands from Iroha block
 * @param block - Iroha block
 * @return list full of "set account detail" commands
 */
fun getSetDetailCommands(block: BlockOuterClass.Block): List<Commands.Command> {
    return block.blockV1.payload.transactionsList.flatMap { tx ->
        tx.payload.reducedPayload.commandsList
    }.filter { command -> command.hasSetAccountDetail() }
}

/**
 * Return all "set account detail" commands with its creator from Iroha block
 * @param block - Iroha block
 * @return list full of "set account detail" commands
 */
fun getSetDetailCommandsWithCreator(block: BlockOuterClass.Block): List<CommandWithCreator> {
    return block.blockV1.payload.transactionsList
        .map { tx ->
            tx.payload.reducedPayload.commandsList.map { command ->
                Pair(command, tx.payload.reducedPayload.creatorAccountId)
            }
        }
        .flatten()
        .filter { (command, _) -> command.hasSetAccountDetail() }
        .map { (command, creator) -> CommandWithCreator(command, creator) }
}

/**
 * Return all "create account" commands from Iroha block
 * @param block - Iroha block
 * @return list full of "create account" commands
 */
fun getCreateAccountCommands(block: BlockOuterClass.Block): List<Commands.Command> {
    return block.blockV1.payload.transactionsList.flatMap { tx ->
        tx.payload.reducedPayload.commandsList
    }.filter { command -> command.hasCreateAccount() }
}

/**
 * Return all "transfer asset" commands from Iroha block
 * @param block - Iroha block
 * @return list full of "transfer asset" commands
 */
fun getTransferCommands(block: BlockOuterClass.Block): List<Commands.Command> {
    return block.blockV1.payload.transactionsList.flatMap { tx -> tx.payload.reducedPayload.commandsList }
        .filter { command -> command.hasTransferAsset() }
}

/**
 * Return transactions with "transfer asset" commands from Iroha block
 * @param block - Iroha block
 * @return list full of transactions with "transfer asset" commands
 */
fun getTransferTransactions(block: BlockOuterClass.Block): List<TransactionOuterClass.Transaction> {
    return block.blockV1.payload.transactionsList
        .filter { tx ->
            tx.payload.reducedPayload.commandsList
                .any { command -> command.hasTransferAsset() }
        }
}

/**
 * Return transactions with "set account detail" commands from Iroha block
 * @param block - Iroha block
 * @return list full of transactions with "set account detail" commands
 */
fun getSetAccountDetailTransactions(block: BlockOuterClass.Block): List<TransactionOuterClass.Transaction> {
    return block.blockV1.payload.transactionsList
        .filter { tx ->
            tx.payload.reducedPayload.commandsList
                .any { command -> command.hasSetAccountDetail() }
        }
}

/**
 * Return all withdrawal transactions from Iroha block
 * @param block - Iroha block
 * @param withdrawalAccount - account that is used to perform withdrawals
 * @return list full of "transfer asset" transactions
 */
fun getWithdrawalTransactions(
    block: BlockOuterClass.Block,
    withdrawalAccount: String
): List<TransactionOuterClass.Transaction> {
    return block.blockV1.payload.transactionsList
        .filter { tx ->
            tx.payload.reducedPayload.commandsList
                .any { it.hasTransferAsset() && it.transferAsset.destAccountId == withdrawalAccount }
        }
}

/**
 * Check if transaction is withdrawal transaction.
 * Transaction is withdrawal when it has many commands including
 * transfer to withdrawal account with amount to withdraw
 */
fun isWithdrawalTransaction(
    transaction: TransactionOuterClass.Transaction,
    withdrawalAccountId: String
): Boolean {
    val commands = transaction.payload.reducedPayload.commandsList
    return commands.any { cmd -> cmd.hasTransferAsset() && cmd.transferAsset.destAccountId == withdrawalAccountId }
}

/**
 * Retrieves withdrawal commands from initial transaction
 */
fun getWithdrawalCommands(
    transaction: TransactionOuterClass.Transaction,
    withdrawalAccountId: String
): List<Commands.Command> {
    val commands = transaction.payload.reducedPayload.commandsList
    return commands.filter { cmd -> cmd.hasTransferAsset() && cmd.transferAsset.destAccountId == withdrawalAccountId }
}

/**
 * Data class that represents Iroha command with its creator
 */
data class CommandWithCreator(val command: Commands.Command, val creator: String)
