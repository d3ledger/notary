package com.d3.commons.expansion

import com.d3.commons.util.irohaUnEscape
import com.google.gson.Gson
import iroha.protocol.BlockOuterClass
import iroha.protocol.Commands
import jp.co.soramitsu.bootstrap.changelog.ChangelogInterface
import jp.co.soramitsu.bootstrap.changelog.ExpansionDetails
import jp.co.soramitsu.iroha.java.IrohaAPI
import mu.KLogging

/**
 * Service expansion class
 * @param expansionTriggerAccountId - account is used as an expansion trigger
 * @param irohaAPI - Iroha API
 */
class ServiceExpansion(
    private val expansionTriggerAccountId: String,
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
        expansionLogic: (ExpansionDetails) -> Unit = {}
    ) {
        block.blockV1.payload.transactionsList
            // Get superuser transactions
            .filter { tx -> tx.payload.reducedPayload.creatorAccountId == ChangelogInterface.superuserAccountId }
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
                expansionLogic(expansionDetails)
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
