/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.handler

import com.d3.notifications.config.NotificationsConfig
import com.d3.notifications.event.ECDSASignature
import com.d3.notifications.event.EthWithdrawalProofsEvent
import com.d3.notifications.provider.ETH_WITHDRAWAL_PROOF_DOMAIN
import com.d3.notifications.provider.EthWithdrawalProof
import com.d3.notifications.provider.EthWithdrawalProofProvider
import com.d3.notifications.queue.EventsQueue
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import mu.KLogging
import org.springframework.stereotype.Component

/**
 * Command handler that handles 'enough withdrawal proofs collected' events
 */
@Component
class EthProofsCollectedCommandHandler(
    private val notificationsConfig: NotificationsConfig,
    private val ethWithdrawalProofProvider: EthWithdrawalProofProvider,
    private val eventsQueue: EventsQueue
) : CommandHandler() {

    // Collection that stores proof command that have been handled already
    private val handledProofs = HashSet<String>()

    override fun handle(commandWithTx: CommandWithTx) {
        val command = commandWithTx.command
        val proofStorageAccount = command.setAccountDetail.accountId
        val savedProofs = ArrayList<EthWithdrawalProof>()
        ethWithdrawalProofProvider.getAllProofs(proofStorageAccount)
            .flatMap { proofs ->
                savedProofs.addAll(proofs)
                ethWithdrawalProofProvider.enoughProofs(proofs)
            }.map { enoughProofs ->
                if (enoughProofs) {
                    val firstProof = savedProofs.first()
                    val ethWithdrawalProofsEvent = EthWithdrawalProofsEvent(
                        accountIdToNotify = firstProof.account,
                        tokenContractAddress = firstProof.tokenContractAddress,
                        amount = firstProof.amount,
                        id = proofStorageAccount + "_eth_proofs",
                        time = commandWithTx.tx.payload.reducedPayload.createdTime,
                        proofs = savedProofs.map { ECDSASignature(r = it.r, s = it.s, v = it.v) }.toList(),
                        relay = firstProof.relay,
                        irohaTxHash = firstProof.irohaHash
                    )
                    logger.info("Notify withdrawal proofs collected $ethWithdrawalProofsEvent")
                    eventsQueue.enqueue(ethWithdrawalProofsEvent)
                    handledProofs.add(proofStorageAccount)
                } else {
                    logger.warn("Not enough withdrawal proofs collected")
                }
            }.failure { ex -> logger.error("Cannot handle withdrawal proofs in Eth", ex) }
    }

    override fun ableToHandle(commandWithTx: CommandWithTx): Boolean {
        if (!commandWithTx.command.hasSetAccountDetail()) {
            return false
        }
        val creator = commandWithTx.tx.payload.reducedPayload.creatorAccountId
        return creator == notificationsConfig.notaryCredential.accountId &&
                !handledProofs.contains(commandWithTx.command.setAccountDetail.accountId) &&
                commandWithTx.command.setAccountDetail.accountId.endsWith("@$ETH_WITHDRAWAL_PROOF_DOMAIN")
    }

    companion object : KLogging()
}
