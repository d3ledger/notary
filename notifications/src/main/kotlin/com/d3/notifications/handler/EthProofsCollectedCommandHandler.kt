/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.handler

import org.springframework.stereotype.Component

/**
 * Command handler that handles 'enough withdrawal proofs collected' events
 */
@Component
class EthProofsCollectedCommandHandler : CommandHandler() {
    override fun handle(commandWithTx: CommandWithTx) {
        //TODO implement this thing
    }

    override fun ableToHandle(commandWithTx: CommandWithTx): Boolean {
        //TODO implement this thing
        return false
    }
}
