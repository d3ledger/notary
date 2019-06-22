/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.expansion

import com.d3.commons.util.irohaUnEscape
import com.google.gson.Gson
import iroha.protocol.BlockOuterClass
import iroha.protocol.Commands
import jp.co.soramitsu.iroha.java.IrohaAPI
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
     * @param expansionLogic - additional expansion logic
     */
    fun expand(
        block: BlockOuterClass.Block,
        expansionLogic: (ExpansionDetails, Long) -> Unit = { _, _ -> }
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
                expansionLogic(expansionDetails, block.blockV1.payload.createdTime)
            }
    }


    /**
     * Checks if command is 'expansion' command
     * @param setAccountDetail - 'setAccountDetail' command that is checked to say if it's 'expansion' command
     */
    private fun isExpansionEvent(setAccountDetail: Commands.SetAccountDetail) =
        setAccountDetail.accountId == expansionTriggerAccountId

    companion object : KLogging()
}
