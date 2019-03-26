package com.d3.btc.withdrawal.handler

import com.d3.btc.helper.currency.btcToSat
import com.d3.btc.withdrawal.config.BtcWithdrawalConfig
import com.d3.btc.withdrawal.provider.WithdrawalConsensusProvider
import com.d3.btc.withdrawal.service.BtcRollbackService
import com.d3.btc.withdrawal.transaction.WithdrawalDetails
import iroha.protocol.Commands
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Handler that handles Iroha Bitcoin transfers
 */
@Component
class NewTransferHandler(
    @Autowired private val btcWithdrawalConfig: BtcWithdrawalConfig,
    @Autowired private val withdrawalConsensusProvider: WithdrawalConsensusProvider,
    @Autowired private val btcRollbackService: BtcRollbackService
) {

    /**
     * Handles "transfer asset" command
     * @param transferCommand - object with "transfer asset" data: source account, destination account, amount and etc
     */
    fun handleTransferCommand(transferCommand: Commands.TransferAsset, withdrawalTime: Long) {
        if (transferCommand.destAccountId != btcWithdrawalConfig.withdrawalCredential.accountId) {
            return
        }
        val destinationAddress = transferCommand.description
        val sourceAccountId = transferCommand.srcAccountId
        val btcAmount = BigDecimal(transferCommand.amount)
        val satAmount = btcToSat(btcAmount)
        val withdrawalDetails = WithdrawalDetails(sourceAccountId, destinationAddress, satAmount, withdrawalTime)
        //Create consensus
        withdrawalConsensusProvider.createConsensusData(withdrawalDetails).fold({
            logger.info("Consensus data for $withdrawalDetails has beenl created")
        }, { ex ->
            logger.error("Cannot create consensus for withdrawal $withdrawalDetails", ex)
            btcRollbackService.rollback(withdrawalDetails, "Cannot create consensus")
        })
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
