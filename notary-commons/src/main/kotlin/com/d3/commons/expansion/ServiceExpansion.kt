/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.expansion

import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.consumer.MultiSigIrohaConsumer
import com.d3.commons.util.irohaUnEscape
import com.d3.commons.util.unHex
import com.google.gson.Gson
import iroha.protocol.BlockOuterClass
import iroha.protocol.Commands
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Transaction
import mu.KLogging

/**
 * Service expansion class
 * @param expansionTriggerAccountId - account is used as an expansion trigger
 * @param expansionTriggerCreatorAccountId - account id that creates an expansion trigger
 * @param irohaAPI - Iroha API
 */
class ServiceExpansion(
    private val expansionTriggerAccountId: String,
    private val expansionTriggerCreatorAccountId: String,
    private val irohaAPI: IrohaAPI
) {

    private val gson = Gson()

    /**
     * Expands given accounts quorum
     * @param block - Iroha blocks that is used to check if there is a 'expansion' transaction made by superuser
     * @param accountsToExpand - accounts that will be expanded
     * @param additionalLogic - additional expansion logic
     */
    fun expand(
        block: BlockOuterClass.Block,
        accountsToExpand: List<IrohaCredential>,
        additionalLogic: (ExpansionDetails) -> Unit = {}
    ) {
        block.blockV1.payload.transactionsList
            // Get superuser transactions
            .filter { tx -> tx.payload.reducedPayload.creatorAccountId == expansionTriggerCreatorAccountId }
            // Get commands
            .flatMap { tx -> tx.payload.reducedPayload.commandsList }
            // Get set account details
            .filter { command -> command.hasSetAccountDetail() }
            .map { command -> command.setAccountDetail }
            // Get expansion details
            .filter { setAccountDetail -> isExpansionEvent(setAccountDetail) }
            .map { setAccountDetail ->
                gson.fromJson(
                    setAccountDetail.value.irohaUnEscape(),
                    ExpansionDetails::class.java
                )
            }
            .forEach { expansionDetails ->
                // Get account that needs expansion
                val account =
                    accountsToExpand.find { account -> account.accountId == expansionDetails.accountIdToExpand }
                if (account != null) {
                    // Expand account
                    expandAccount(account, expansionDetails, block.blockV1.payload.createdTime)
                    additionalLogic(expansionDetails)
                } else {
                    logger.warn("No account with id ${expansionDetails.accountIdToExpand} was found. Cannot expand.")
                }
            }
    }

    /**
     * Expands given account
     * @param account - account to expand
     * @param expansionDetails - details of expansion
     * @param txTime - time of 'expansion' transaction. Must be the same on all the nodes.
     */
    private fun expandAccount(
        account: IrohaCredential,
        expansionDetails: ExpansionDetails,
        txTime: Long
    ) {
        val consumer = MultiSigIrohaConsumer(account, irohaAPI)
        consumer.send(
            createExpandingTransaction(
                account,
                expansionDetails,
                txTime,
                consumer.getConsumerQuorum().get()
            )
        ).fold(
            { txHash ->
                logger.info(
                    "Expanding transaction for account ${account.accountId} has been sent." +
                            " Expansion details $expansionDetails. Tx hash $txHash"
                )
            },
            { ex ->
                logger.error(
                    "Cannot expand account ${account.accountId}." +
                            " Expansion details $expansionDetails",
                    ex
                )
            })
    }

    /**
     * Creates 'expansion' transaction
     * @param account - account to expand
     * @param expansionDetails - details of expansion
     * @param txTime - time of transaction
     * @param currentQuorum - expanded account current quorum
     */
    private fun createExpandingTransaction(
        account: IrohaCredential,
        expansionDetails: ExpansionDetails, txTime: Long,
        currentQuorum: Int
    ) = Transaction.builder(account.accountId)
        .addSignatory(account.accountId, String.unHex(expansionDetails.publicKey.toLowerCase()))
        .setAccountQuorum(account.accountId, expansionDetails.quorum)
        .setQuorum(currentQuorum)
        .setCreatedTime(txTime)
        .build()

    /**
     * Checks if command is 'expansion' command
     * @param setAccountDetail - 'setAccountDetail' command that is checked to say if it's 'expansion' command
     */
    private fun isExpansionEvent(setAccountDetail: Commands.SetAccountDetail) =
        setAccountDetail.accountId == expansionTriggerAccountId

    companion object : KLogging()
}
