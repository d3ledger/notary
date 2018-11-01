package withdrawal.transaction

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import org.bitcoinj.core.Address
import org.bitcoinj.core.Transaction
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import provider.btc.network.BtcNetworkConfigProvider

/*
    Class that is used to create BTC transactions
 */
@Component
class TransactionCreator(
    @Autowired private val changeAddress: Address,
    @Autowired private val btcNetworkConfigProvider: BtcNetworkConfigProvider,
    @Autowired private val transactionHelper: TransactionHelper
) {

    /**
     * Creates UNSIGNED Bitcoin transaction
     * @param wallet - current wallet that will be used to fetch unspent transactions
     * @param amount - amount of money to spend
     * @param destinationAddress - receiver's base58 Bitcoin address
     * @param confidenceLevel - minimum tx depth that will be used in unspents
     * @return result with unsigned transaction full of input/output data
     */
    fun createTransaction(
        wallet: Wallet,
        amount: Long,
        destinationAddress: String,
        confidenceLevel: Int
    ): Result<Transaction, Exception> {
        val transaction = Transaction(btcNetworkConfigProvider.getConfig())
        return transactionHelper.getAvailableAddresses(wallet).flatMap { availableAddresses ->
            transactionHelper.collectUnspents(availableAddresses, wallet, amount, confidenceLevel)
        }.map { unspents ->
            unspents.forEach { unspent -> transaction.addInput(unspent) }
            unspents
        }.map { unspents ->
            transactionHelper.addOutputs(transaction, unspents, destinationAddress, amount, changeAddress)
            unspents
        }.map { unspents ->
            transactionHelper.registerUnspents(unspents)
        }.map { transaction }
    }
}
