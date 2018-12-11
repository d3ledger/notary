package withdrawal.btc.handler

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import helper.address.isValidBtcAddress
import helper.currency.btcToSat
import iroha.protocol.Commands
import monitoring.Monitoring
import mu.KLogging
import org.bitcoinj.core.Transaction
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import withdrawal.btc.config.BtcWithdrawalConfig
import withdrawal.btc.provider.BtcWhiteListProvider
import withdrawal.btc.statistics.WithdrawalStatistics
import withdrawal.btc.transaction.SignCollector
import withdrawal.btc.transaction.TransactionCreator
import withdrawal.btc.transaction.UnsignedTransactions
import java.math.BigDecimal

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
    @Autowired private val unsignedTransactions: UnsignedTransactions
) : Monitoring() {
    override fun monitor() = withdrawalStatistics

    private val newBtcTransactionListeners = ArrayList<(tx: Transaction) -> Unit>()

    /**
     * Registers "new transaction was created" event listener
     * Not thread safe. For testing purposes only.
     * @param listener - function that will be called when new transaction is created
     */
    fun addNewBtcTransactionListener(listener: (tx: Transaction) -> Unit) {
        newBtcTransactionListeners.add(listener)
    }

    /**
     * Handles "transfer asset" command
     * @param transferCommand - object with "transfer asset" data: source account, destination account, amount and etc
     */
    fun handleTransferCommand(wallet: Wallet, transferCommand: Commands.TransferAsset) {
        if (transferCommand.destAccountId != btcWithdrawalConfig.withdrawalCredential.accountId) {
            return
        }
        val destinationAddress = transferCommand.description
        if (!isValidBtcAddress(destinationAddress)) {
            logger.warn { "Cannot execute transfer. Destination $destinationAddress is not a valid base58 address." }
            return
        }
        val btcAmount = BigDecimal(transferCommand.amount)
        logger.info {
            "Withdrawal event(" +
                    "from:${transferCommand.srcAccountId} " +
                    "to:$destinationAddress " +
                    "amount:${btcAmount.toPlainString()})"
        }
        withdrawalStatistics.incTotalTransfers()
        whiteListProvider.checkWithdrawalAddress(transferCommand.srcAccountId, destinationAddress)
            .fold({ ableToWithdraw ->
                if (ableToWithdraw) {
                    withdraw(wallet, destinationAddress, btcToSat(btcAmount))
                } else {
                    logger.warn { "Cannot withdraw to $destinationAddress, because it's not in ${transferCommand.srcAccountId} whitelist" }
                }
            }, { ex ->
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
    private fun withdraw(wallet: Wallet, destinationAddress: String, amount: Long) {
        transactionCreator.createTransaction(
            wallet,
            amount,
            destinationAddress,
            btcWithdrawalConfig.bitcoin.confidenceLevel
        ).map { transaction ->
            newBtcTransactionListeners.forEach { listener ->
                listener(transaction)
            }
            transaction
        }.map { transaction ->
            logger.info { "Tx to sign\n$transaction" }
            signCollector.collectSignatures(transaction, btcWithdrawalConfig.bitcoin.walletPath)
            transaction
        }.map { transaction ->
            unsignedTransactions.markAsUnsigned(transaction)
            logger.info { "Tx ${transaction.hashAsString} was added to collection of unsigned transactions" }
        }.failure { ex ->
            withdrawalStatistics.incFailedTransfers()
            logger.error("Cannot create withdrawal transaction", ex)
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
