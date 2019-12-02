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
    override fun handle(irohaCommand: IrohaCommand) =
        handleFailedRegistrationEventNotification(irohaCommand, RegistrationEventSubsystem.ETH, eventsQueue)

    override fun ableToHandle(irohaCommand: IrohaCommand) =
        isFailedRegistration(irohaCommand, notificationsConfig.ethRegistrationServiceAccount, notaryClientsProvider)
}


/**
 * Checks if command is a 'failed registration' command
 * @param irohaCommand - command
 * @param registrationAccount - account that register clients in sidechains
 * @param notaryClientsProvider - provider that is used to get clients
 * @return true if a command is a 'failed registration' command
 */
private fun isFailedRegistration(
    irohaCommand: IrohaCommand,
    registrationAccount: String,
    notaryClientsProvider: NotaryClientsProvider
) = safeCheck {
    return irohaCommand.command.hasSetAccountDetail() &&
            irohaCommand.tx.getCreator() == registrationAccount &&
            irohaCommand.command.setAccountDetail.key == FAILED_REGISTRATION_KEY &&
            notaryClientsProvider.isClient(irohaCommand.command.setAccountDetail.accountId).get()
}

/**
 * Handles 'failed registration' event
 * @param irohaCommand - command
 * @param subsystem - registration subsystem(Ethereum, Bitcoin, etc),
 * @param eventsQueue - queue of events
 */
private fun handleFailedRegistrationEventNotification(
    irohaCommand: IrohaCommand,
    subsystem: RegistrationEventSubsystem,
    eventsQueue: EventsQueue
) {
    val failedRegistrationNotifyEvent =
        FailedRegistrationNotifyEvent(
            subsystem = subsystem,
            accountId = irohaCommand.command.setAccountDetail.accountId,
            id = Utils.toHex(Utils.hash(irohaCommand.tx)) + "_failed_registration",
            txTime = irohaCommand.tx.payload.reducedPayload.createdTime,
            blockNum = irohaCommand.block.blockV1.payload.height,
            txIndex = irohaCommand.getTxIndex()
        )
    eventsQueue.enqueue(failedRegistrationNotifyEvent)
}
