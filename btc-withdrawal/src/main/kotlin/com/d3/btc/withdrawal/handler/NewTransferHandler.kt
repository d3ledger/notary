package com.d3.btc.withdrawal.handler

import com.d3.btc.fee.CurrentFeeRate
import com.d3.btc.helper.address.isValidBtcAddress
import com.d3.btc.helper.currency.btcToSat
import com.d3.btc.withdrawal.config.BtcWithdrawalConfig
import com.d3.btc.withdrawal.provider.WithdrawalConsensusProvider
import com.d3.btc.withdrawal.service.BtcRollbackService
import com.d3.btc.withdrawal.statistics.WithdrawalStatistics
import com.d3.btc.withdrawal.transaction.TransactionHelper
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
    @Autowired private val withdrawalStatistics: WithdrawalStatistics,
    @Autowired private val btcWithdrawalConfig: BtcWithdrawalConfig,
    @Autowired private val withdrawalConsensusProvider: WithdrawalConsensusProvider,
    @Autowired private val btcRollbackService: BtcRollbackService,
    @Autowired private val transactionHelper: TransactionHelper
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
        val withdrawalDetails =
            WithdrawalDetails(sourceAccountId, destinationAddress, satAmount, withdrawalTime)

        logger.info {
            "Withdrawal event(" +
                    "from:$sourceAccountId " +
                    "to:$destinationAddress " +
                    "amount:${btcAmount.toPlainString()}" +
                    "hash:${withdrawalDetails.irohaFriendlyHashCode()})"
        }

        if (!CurrentFeeRate.isPresent()) {
            logger.warn { "Cannot execute transfer. Fee rate was not set." }
            btcRollbackService.rollback(
                sourceAccountId,
                satAmount,
                withdrawalTime,
                "Not able to transfer yet"
            )
            return
        }
        // Check if withdrawal has valid destination address
        if (!isValidBtcAddress(destinationAddress)) {
            logger.warn { "Cannot execute transfer. Destination $destinationAddress is not a valid base58 address." }
            btcRollbackService.rollback(
                sourceAccountId,
                satAmount,
                withdrawalTime,
                "Invalid address"
            )
            return
        }
        // Check if withdrawal amount is not too little
        if (transactionHelper.isDust(satAmount)) {
            btcRollbackService.rollback(
                sourceAccountId,
                satAmount,
                withdrawalTime,
                "Too small amount"
            )
            logger.warn { "Can't spend SAT $satAmount, because it's considered a dust" }
            return
        }

        // Create consensus
        withdrawalStatistics.incTotalTransfers()
        startConsensusProcess(withdrawalDetails)
    }

    /**
     * Starts consensus creation process
     * @param withdrawalDetails - details of withdrawal
     */
    private fun startConsensusProcess(withdrawalDetails: WithdrawalDetails) {
        withdrawalConsensusProvider.createConsensusData(withdrawalDetails).fold({
            logger.info("Consensus data for $withdrawalDetails has been created")
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
