package com.d3.btc.withdrawal.transaction

import com.d3.btc.provider.network.BtcNetworkConfigProvider
import com.d3.btc.withdrawal.provider.BtcChangeAddressProvider
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import mu.KLogging
import org.bitcoinj.core.Address
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionOutput
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/*
    Class that is used to create BTC transactions
 */
@Component
class TransactionCreator(
    @Autowired private val btcChangeAddressProvider: BtcChangeAddressProvider,
    @Autowired private val btcNetworkConfigProvider: BtcNetworkConfigProvider,
    @Autowired private val transactionHelper: TransactionHelper
) {

    /**
     * Creates UNSIGNED Bitcoin transaction
     * @param wallet - current wallet that will be used to fetch unspent transactions
     * @param amount - amount of SAT to spend
     * @param destinationAddress - receiver's base58 Bitcoin address
     * @param confidenceLevel - minimum tx depth that will be used in unspents
     * @return result with unsigned transaction full of input/output data and used unspents
     */
    fun createTransaction(
        wallet: Wallet,
        amount: Long,
        destinationAddress: String,
        confidenceLevel: Int
    ): Result<Pair<Transaction, List<TransactionOutput>>, Exception> {
        val transaction = Transaction(btcNetworkConfigProvider.getConfig())
        return transactionHelper.getAvailableAddresses(wallet).flatMap { availableAddresses ->
            logger.info("Available addresses $availableAddresses")
            transactionHelper.collectUnspents(availableAddresses, wallet, amount, confidenceLevel)
        }.fanout {
            btcChangeAddressProvider.getChangeAddress()
        }.map { (unspents, changeAddress) ->
            unspents.forEach { unspent -> transaction.addInput(unspent) }
            transactionHelper.addOutputs(
                transaction,
                unspents,
                destinationAddress,
                amount,
                Address.fromBase58(btcNetworkConfigProvider.getConfig(), changeAddress.address)
            )
            unspents
        }.map { unspents -> Pair(transaction, unspents) }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
