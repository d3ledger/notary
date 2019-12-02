/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.handler

import iroha.protocol.BlockOuterClass
import iroha.protocol.Commands
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging
import java.util.*
import java.util.stream.IntStream

/**
 * The main command handler class
 */
abstract class CommandHandler {

    /**
     * Handles given command if it's possible
     * @param irohaCommand command to handle
     */
    fun handleCommand(irohaCommand: IrohaCommand) {
        try {
            if (ableToHandle(irohaCommand)) {
                handle(irohaCommand)
            }
        } catch (e: Exception) {
            logger.error("Cannot handle command ${irohaCommand.command}", e)
        }
    }

    /**
     * The main handling logic
     * @param irohaCommand - command to handle
     */
    protected abstract fun handle(irohaCommand: IrohaCommand)

    /**
     * Handling predicate. Checks it's possible to handle
     * @param irohaCommand - command to check
     * @return true if possible to handle, false otherwise
     */
    protected abstract fun ableToHandle(irohaCommand: IrohaCommand): Boolean

    companion object : KLogging()
}

/**
 * Data class that represents Iroha command
 * @param command - command itself
 * @param tx - tx that has given command inside it
 * @param block - block that has given command and tx inside it
 */
data class IrohaCommand(
    val command: Commands.Command,
    val tx: TransactionOuterClass.Transaction,
    val block: BlockOuterClass.Block
) {
    /**
     * Returns index of the current tx in the current block
     */
    fun getTxIndex(): Int {
        val myHash = Utils.hash(tx)
        val txIndex = IntStream.range(0, block.blockV1.payload.transactionsList.size).filter {
            Arrays.equals(myHash, Utils.hash(block.blockV1.payload.transactionsList[it]))
        }.findFirst()
        if (txIndex.isPresent) {
            return txIndex.asInt
        }
        throw IllegalArgumentException("Tx $tx is not present in block ${block.blockV1.payload.height}")
    }
}


/**
 * Returns tx creator
 */
fun TransactionOuterClass.Transaction.getCreator() = this.payload.reducedPayload.creatorAccountId!!

/**
 * Executes check safely
 */
inline fun safeCheck(check: () -> Boolean): Boolean {
    return try {
        check()
    } catch (e: Exception) {
        CommandHandler.logger.error("Cannot check", e)
        false
    }
}
