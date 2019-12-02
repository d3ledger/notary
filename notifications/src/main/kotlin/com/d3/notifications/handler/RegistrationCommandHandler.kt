/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.handler

import com.d3.commons.provider.NotaryClientsProvider
import com.d3.notifications.config.NotificationsConfig
import com.d3.notifications.event.RegistrationEventSubsystem
import com.d3.notifications.event.RegistrationNotifyEvent
import com.d3.notifications.init.ETH_WALLET
import com.d3.notifications.queue.EventsQueue
import jp.co.soramitsu.iroha.java.Utils
import org.springframework.stereotype.Component

/**
 * Handler that handles registration events in Ethereum
 */
@Component
class EthRegistrationCommandHandler(
    private val eventsQueue: EventsQueue,
    private val notaryClientsProvider: NotaryClientsProvider,
    private val notificationsConfig: NotificationsConfig
) : CommandHandler() {

    override fun ableToHandle(irohaCommand: IrohaCommand): Boolean {
        return isRegistration(
            irohaCommand,
            notificationsConfig.ethRegistrationServiceAccount,
            ETH_WALLET,
            notaryClientsProvider
        )
    }

    override fun handle(irohaCommand: IrohaCommand) =
        handleRegistrationEventNotification(irohaCommand, RegistrationEventSubsystem.ETH, eventsQueue)
}


/**
 * Checks if command is a registration command
 * @param irohaCommand - Iroha command
 * @param registrationAccount - account that register clients in sidechains
 * @param sideChainWalletKey - key of a sidechain wallet
 * @param notaryClientsProvider - provider of D3 clients
 * @return true if a command is a registration command
 */
private fun isRegistration(
    irohaCommand: IrohaCommand,
    registrationAccount: String,
    sideChainWalletKey: String,
    notaryClientsProvider: NotaryClientsProvider
) = safeCheck {
    return irohaCommand.command.hasSetAccountDetail() &&
            irohaCommand.tx.getCreator() == registrationAccount &&
            irohaCommand.command.setAccountDetail.key == sideChainWalletKey &&
            notaryClientsProvider.isClient(irohaCommand.command.setAccountDetail.accountId).get()
}

/**
 * Handles registration event
 * @param irohaCommand - command with registration event
 * @param subsystem - registration subsystem(Ethereum, Bitcoin, etc)
 */
private fun handleRegistrationEventNotification(
    irohaCommand: IrohaCommand,
    subsystem: RegistrationEventSubsystem,
    eventsQueue: EventsQueue
) {
    val registrationNotifyEvent =
        RegistrationNotifyEvent(
            subsystem = subsystem,
            accountId = irohaCommand.command.setAccountDetail.accountId,
            address = irohaCommand.command.setAccountDetail.value,
            id = Utils.toHex(Utils.hash(irohaCommand.tx)) + "_registration",
            txTime = irohaCommand.tx.payload.reducedPayload.createdTime,
            blockNum = irohaCommand.block.blockV1.payload.height,
            txIndex = irohaCommand.getTxIndex()
        )
    eventsQueue.enqueue(registrationNotifyEvent)
}
