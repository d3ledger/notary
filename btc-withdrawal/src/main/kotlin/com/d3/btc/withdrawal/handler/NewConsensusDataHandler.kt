package com.d3.btc.withdrawal.handler

import com.d3.btc.helper.address.getSignThreshold
import com.d3.btc.withdrawal.service.WithdrawalTransferService
import com.d3.btc.withdrawal.transaction.consensus.ConsensusDataStorage
import com.d3.btc.withdrawal.transaction.consensus.WithdrawalConsensus
import com.d3.commons.sidechain.iroha.BTC_CONSENSUS_DOMAIN
import com.d3.commons.util.irohaUnEscape
import iroha.protocol.Commands
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

//TODO refactor handlers
/**
 * Handler that handles new consensus data appearance
 */
@Component
class NewConsensusDataHandler(
    @Autowired private val withdrawalTransferService: WithdrawalTransferService
) {

    /**
     * Handles new consensus command
     * @param addConsensusCommand - new consensus command
     */
    fun handleNewConsensusCommand(addConsensusCommand: Commands.SetAccountDetail) {
        val withdrawalHash = addConsensusCommand.accountId.replace("@$BTC_CONSENSUS_DOMAIN", "")
        val withdrawalConsensus = WithdrawalConsensus.fromJson(addConsensusCommand.value.irohaUnEscape())
        if (!ConsensusDataStorage.add(withdrawalHash, withdrawalConsensus)) {
            logger.warn("Cannot add consensus data. Withdrawal with hash $withdrawalHash probably was handled before")
            return
        }
        val consensusData = ConsensusDataStorage.get(withdrawalHash)!!
        val threshold = getSignThreshold(withdrawalConsensus.peers)
        if (consensusData.consensus.size < threshold) {
            logger.info(
                "Not enough consensus data was collected for withdrawal with hash $withdrawalHash. " +
                        "Need at least $threshold"
            )
            return
        } else {
            val commonConsensus = WithdrawalConsensus.createCommonConsensus(consensusData.consensus)
            logger.info("Got common withdrawal consensus $commonConsensus. Start withdrawal operation")
            withdrawalTransferService.withdraw(consensusData.withdrawalDetails, commonConsensus)
            //Clear after withdraw
            ConsensusDataStorage.clear(withdrawalHash)
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}

