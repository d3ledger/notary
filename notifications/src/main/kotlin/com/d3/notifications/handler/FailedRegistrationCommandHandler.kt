/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.handler

import com.d3.commons.provider.NotaryClientsProvider
import com.d3.commons.registration.FAILED_REGISTRATION_KEY
import com.d3.notifications.config.NotificationsConfig
import com.d3.notifications.event.FailedRegistrationNotifyEvent
import com.d3.notifications.event.RegistrationEventSubsystem
import com.d3.notifications.queue.EventsQueue
import jp.co.soramitsu.iroha.java.Utils
import org.springframework.stereotype.Component

/**
 * Handler that handles failed Ethereum registration events
 */
@Component
class FailedEthRegistrationCommandHandler(
    private val notificationsConfig: NotificationsConfig,
    private val notaryClientsProvider: NotaryClientsProvider,
    private val eventsQueue: EventsQueue
) : CommandHandler() {
    override fun handle(commandWithTx: CommandWithTx) =
        handleFailedRegistrationEventNotification(commandWithTx, RegistrationEventSubsystem.ETH, eventsQueue)

    override fun ableToHandle(commandWithTx: CommandWithTx) =
        isFailedRegistration(commandWithTx, notificationsConfig.ethRegistrationServiceAccount, notaryClientsProvider)
}

/**
 * Handler that handles failed Bitcoin registration events
 */
@Component
class FailedBtcRegistrationCommandHandler(
    private val notificationsConfig: NotificationsConfig,
    private val notaryClientsProvider: NotaryClientsProvider,
    private val eventsQueue: EventsQueue
) : CommandHandler() {
    override fun handle(commandWithTx: CommandWithTx) =
        handleFailedRegistrationEventNotification(commandWithTx, RegistrationEventSubsystem.BTC, eventsQueue)

    override fun ableToHandle(commandWithTx: CommandWithTx) =
        isFailedRegistration(commandWithTx, notificationsConfig.btcRegistrationServiceAccount, notaryClientsProvider)
}

/**
 * Checks if command is a 'failed registration' command
 * @param commandWithTx - command
 * @param registrationAccount - account that register clients in sidechains
 * @param notaryClientsProvider - provider that is used to get clients
 * @return true if a command is a 'failed registration' command
 */
private fun isFailedRegistration(
    commandWithTx: CommandWithTx,
    registrationAccount: String,
    notaryClientsProvider: NotaryClientsProvider
) = safeCheck {
    return commandWithTx.command.hasSetAccountDetail() &&
            commandWithTx.tx.getCreator() == registrationAccount &&
            commandWithTx.command.setAccountDetail.key == FAILED_REGISTRATION_KEY &&
            notaryClientsProvider.isClient(commandWithTx.command.setAccountDetail.accountId).get()
}

/**
 * Handles 'failed registration' event
 * @param commandWithTx - command
 * @param subsystem - registration subsystem(Ethereum, Bitcoin, etc),
 * @param eventsQueue - queue of events
 */
private fun handleFailedRegistrationEventNotification(
    commandWithTx: CommandWithTx,
    subsystem: RegistrationEventSubsystem,
    eventsQueue: EventsQueue
) {
    val failedRegistrationNotifyEvent =
        FailedRegistrationNotifyEvent(
            subsystem = subsystem,
            accountId = commandWithTx.command.setAccountDetail.accountId,
            id = Utils.toHex(Utils.hash(commandWithTx.tx)) + "_failed_registration",
            time = commandWithTx.tx.payload.reducedPayload.createdTime
        )
    eventsQueue.enqueue(failedRegistrationNotifyEvent)
}
