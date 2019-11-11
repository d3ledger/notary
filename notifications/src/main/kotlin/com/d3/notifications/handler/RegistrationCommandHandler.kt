/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.handler

import com.d3.commons.provider.NotaryClientsProvider
import com.d3.notifications.config.NotificationsConfig
import com.d3.notifications.event.RegistrationEventSubsystem
import com.d3.notifications.event.RegistrationNotifyEvent
import com.d3.notifications.init.BTC_WALLET
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

    override fun ableToHandle(commandWithTx: CommandWithTx): Boolean {
        return isRegistration(
            commandWithTx,
            notificationsConfig.ethRegistrationServiceAccount,
            ETH_WALLET,
            notaryClientsProvider
        )
    }

    override fun handle(commandWithTx: CommandWithTx) =
        handleRegistrationEventNotification(commandWithTx, RegistrationEventSubsystem.ETH, eventsQueue)
}

/**
 * Handler that handlers Bitcoin registration events
 */
@Component
class BtcRegistrationCommandHandler(
    private val eventsQueue: EventsQueue,
    private val notaryClientsProvider: NotaryClientsProvider,
    private val notificationsConfig: NotificationsConfig
) : CommandHandler() {

    override fun ableToHandle(commandWithTx: CommandWithTx): Boolean {
        return isRegistration(
            commandWithTx,
            notificationsConfig.btcRegistrationServiceAccount,
            BTC_WALLET,
            notaryClientsProvider
        )
    }

    override fun handle(commandWithTx: CommandWithTx) =
        handleRegistrationEventNotification(commandWithTx, RegistrationEventSubsystem.BTC, eventsQueue)
}

/**
 * Checks if command is a registration command
 * @param commandWithTx - Iroha command
 * @param registrationAccount - account that register clients in sidechains
 * @param sideChainWalletKey - key of a sidechain wallet
 * @param notaryClientsProvider - provider of D3 clients
 * @return true if a command is a registration command
 */
private fun isRegistration(
    commandWithTx: CommandWithTx,
    registrationAccount: String,
    sideChainWalletKey: String,
    notaryClientsProvider: NotaryClientsProvider
) = safeCheck {
    return commandWithTx.command.hasSetAccountDetail() &&
            commandWithTx.tx.getCreator() == registrationAccount &&
            commandWithTx.command.setAccountDetail.key == sideChainWalletKey &&
            notaryClientsProvider.isClient(commandWithTx.command.setAccountDetail.accountId).get()
}

/**
 * Handles registration event
 * @param commandWithTx - command with registration event
 * @param subsystem - registration subsystem(Ethereum, Bitcoin, etc)
 */
private fun handleRegistrationEventNotification(
    commandWithTx: CommandWithTx,
    subsystem: RegistrationEventSubsystem,
    eventsQueue: EventsQueue
) {
    val registrationNotifyEvent =
        RegistrationNotifyEvent(
            subsystem = subsystem,
            accountId = commandWithTx.command.setAccountDetail.accountId,
            address = commandWithTx.command.setAccountDetail.value,
            id = Utils.toHex(Utils.hash(commandWithTx.tx)) + "_registration",
            time = commandWithTx.tx.payload.reducedPayload.createdTime
        )
    eventsQueue.enqueue(registrationNotifyEvent)
}
