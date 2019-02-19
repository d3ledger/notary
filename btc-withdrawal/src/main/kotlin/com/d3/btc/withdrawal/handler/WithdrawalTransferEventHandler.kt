package com.d3.btc.withdrawal.handler

import com.d3.btc.helper.address.isValidBtcAddress
import com.d3.btc.helper.currency.btcToSat
import com.d3.btc.monitoring.Monitoring
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import iroha.protocol.Commands
import mu.KLogging
import org.bitcoinj.core.Transaction
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.d3.btc.withdrawal.config.BtcWithdrawalConfig
import com.d3.btc.withdrawal.provider.BtcWhiteListProvider
import com.d3.btc.withdrawal.statistics.WithdrawalStatistics
import com.d3.btc.withdrawal.transaction.*
import java.math.BigDecimal
import java.util.concurrent.CopyOnWriteArrayList

/*
    Class that is used to handle withdrawal events
 */
@Component
class WithdrawalTransferEventHandler(
    @Autowired private val withdrawalStatistics: WithdrawalStatistics,
    @Autowired private val whiteListProvider: BtcWhiteListProvider,
    @Autowired private val btcWithdrawalConfig: BtcWithdrawalConfig,
    @Autowired private val transactionCreator: TransactionCreator,
    @Autowired private val signCollector: SignCollector,
    @Autowired private val unsignedTransactions: UnsignedTransactions,
    @Autowired private val transactionHelper: TransactionHelper,
    @Autowired private val btcRollbackService: BtcRollbackService
) : Monitoring() {
    override fun monitor() = withdrawalStatistics

    private val newBtcTransactionListeners = CopyOnWriteArrayList<(tx: Transaction) -> Unit>()

    /**
     * Registers "new transaction was created" event listener
     * For testing purposes only.
     * @param listener - function that will be called when new transaction is created
     */
    fun addNewBtcTransactionListener(listener: (tx: Transaction) -> Unit) {
        newBtcTransactionListeners.add(listener)
    }

    /**
     * Handles "transfer asset" command
     * @param transferCommand - object with "transfer asset" data: source account, destination account, amount and etc
     */
    fun handleTransferCommand(wallet: Wallet, transferCommand: Commands.TransferAsset, withdrawalTime: Long) {
        if (transferCommand.destAccountId != btcWithdrawalConfig.withdrawalCredential.accountId) {
            return
        }
        val destinationAddress = transferCommand.description
        val sourceAccountId = transferCommand.srcAccountId
        val btcAmount = BigDecimal(transferCommand.amount)
        val satAmount = btcToSat(btcAmount)
        logger.info {
            "Withdrawal event(" +
                    "from:${transferCommand.srcAccountId} " +
                    "to:$destinationAddress " +
                    "amount:${btcAmount.toPlainString()})"
        }
        if (!CurrentFeeRate.isPresent()) {
            logger.warn { "Cannot execute transfer. Fee rate was not set." }
            btcRollbackService.rollback(sourceAccountId, satAmount, withdrawalTime)
            return
        }
        if (!isValidBtcAddress(destinationAddress)) {
            logger.warn { "Cannot execute transfer. Destination $destinationAddress is not a valid base58 address." }
            btcRollbackService.rollback(sourceAccountId, satAmount, withdrawalTime)
            return
        }
        if (transactionHelper.isDust(satAmount)) {
            btcRollbackService.rollback(sourceAccountId, satAmount, withdrawalTime)
            logger.warn { "Can't spend SAT $satAmount, because it's considered a dust" }
            return
        }
        withdrawalStatistics.incTotalTransfers()
        whiteListProvider.checkWithdrawalAddress(sourceAccountId, destinationAddress)
            .fold({ ableToWithdraw ->
                if (ableToWithdraw) {
                    withdraw(wallet, destinationAddress, WithdrawalDetails(sourceAccountId, satAmount, withdrawalTime))
                } else {
                    btcRollbackService.rollback(sourceAccountId, satAmount, withdrawalTime)
                    logger.warn { "Cannot withdraw to $destinationAddress, because it's not in ${transferCommand.srcAccountId} whitelist" }
                }
            }, { ex ->
                btcRollbackService.rollback(sourceAccountId, satAmount, withdrawalTime)
                withdrawalStatistics.incFailedTransfers()
                logger.error("Cannot check ability to withdraw", ex)
            })
    }

    /**
     * Starts withdrawal process. Consists of the following steps:
     * 1) Create transaction
     * 2) Call all "on new transaction" listeners
     * 3) Collect transaction input signatures using current node controlled private keys
     * 4) Mark created transaction as unsigned
     * @param wallet - current wallet. Used to obtain private keys
     * @param destinationAddress - Bitcoin address to send money to
     * @param amount - amount of SAT to send
     * */
    private fun withdraw(
        wallet: Wallet,
        destinationAddress: String,
        withdrawalDetails: WithdrawalDetails
    ) {
        transactionCreator.createTransaction(
            wallet,
            withdrawalDetails.amountSat,
            destinationAddress,
            btcWithdrawalConfig.bitcoin.confidenceLevel
        ).map { (transaction, unspents) ->
            newBtcTransactionListeners.forEach { listener ->
                listener(transaction)
            }
            Pair(transaction, unspents)
        }.map { (transaction, unspents) ->
            logger.info { "Tx to sign\n$transaction" }
            signCollector.collectSignatures(transaction, btcWithdrawalConfig.bitcoin.walletPath)
            Pair(transaction, unspents)
        }.map { (transaction, unspents) ->
            unsignedTransactions.markAsUnsigned(withdrawalDetails, transaction)
            transactionHelper.registerUnspents(transaction, unspents)
            logger.info { "Tx ${transaction.hashAsString} was added to collection of unsigned transactions" }
        }.failure { ex ->
            btcRollbackService.rollback(
                withdrawalDetails.sourceAccountId,
                withdrawalDetails.amountSat,
                withdrawalDetails.withdrawalTime
            )
            withdrawalStatistics.incFailedTransfers()
            logger.error("Cannot create withdrawal transaction", ex)
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
