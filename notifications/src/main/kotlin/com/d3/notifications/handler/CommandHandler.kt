/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.handler

import iroha.protocol.Commands
import iroha.protocol.TransactionOuterClass
import mu.KLogging

/**
 * The main command handler class
 */
abstract class CommandHandler {

    /**
     * Handles given command if it's possible
     * @param commandWithTx command to handle
     */
    fun handleCommand(commandWithTx: CommandWithTx) {
        try {
            if (ableToHandle(commandWithTx)) {
                handle(commandWithTx)
            }
        } catch (e: Exception) {
            logger.error("Cannot handle command ${commandWithTx.command}", e)
        }
    }

    /**
     * The main handling logic
     * @param commandWithTx - command to handle
     */
    protected abstract fun handle(commandWithTx: CommandWithTx)

    /**
     * Handling predicate. Checks it's possible to handle
     * @param commandWithTx - command to check
     * @return true if possible to handle, false otherwise
     */
    protected abstract fun ableToHandle(commandWithTx: CommandWithTx): Boolean

    companion object : KLogging()
}

data class CommandWithTx(val command: Commands.Command, val tx: TransactionOuterClass.Transaction)

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
