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
 * Return all "create account" commands from Iroha block
 * @param block - Iroha block
 * @return list full of "create account" commands
 */
fun getCreateAccountCommands(block: BlockOuterClass.Block): List<Commands.Command> {
    return block.blockV1OrBuilder.payloadOrBuilder.transactionsList.flatMap { tx ->
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
 * Check if transaction is withdrawal transaction.
 * Transaction is withdrawal when it has 2 transfer commands:
 * 1) transfer to withdrawal account with amount to withdraw
 * 2) transfer to billing account with fee
 */
fun isWithdrawalTransaction(
    transaction: TransactionOuterClass.Transaction,
    dstAccountId: String
): Boolean {
    val commands = transaction.payload.reducedPayload.commandsList
    return commands.all { cmd -> cmd.hasTransferAsset() && cmd.transferAsset.destAccountId == dstAccountId }
}
