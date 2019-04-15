package com.d3.btc.withdrawal.transaction

import com.d3.btc.model.BtcAddress
import com.d3.btc.provider.network.BtcNetworkConfigProvider
import com.d3.btc.provider.BtcChangeAddressProvider
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import mu.KLogging
import org.bitcoinj.core.Address
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionOutput
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import kotlin.random.Random

//TODO don't forget to restore test
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
     * @param withdrawalDetails - details of withdrawal
     * @param availableHeight - maximum height of UTXO
     * @param confidenceLevel - minimum tx depth that will be used in unspents
     * @return result with unsigned transaction full of input/output data and used unspents
     */
    fun createTransaction(
        withdrawalDetails: WithdrawalDetails,
        availableHeight: Int,
        confidenceLevel: Int
    ): Result<Pair<Transaction, List<TransactionOutput>>, Exception> {
        val transaction = Transaction(btcNetworkConfigProvider.getConfig())
        return transactionHelper.getAvailableAddresses(withdrawalDetails.withdrawalTime).flatMap { availableAddresses ->
            logger.info("Available addresses $availableAddresses")
            transactionHelper.collectUnspents(
                availableAddresses,
                withdrawalDetails.amountSat,
                availableHeight,
                confidenceLevel
            )
        }.fanout {
            btcChangeAddressProvider.getAllChangeAddresses(withdrawalDetails.withdrawalTime)
        }.map { (unspents, changeAddresses) ->

            unspents.forEach { unspent -> transaction.addInput(unspent) }
            val changeAddress = chooseChangeAddress(withdrawalDetails, changeAddresses).address
            logger.info("Change address chosen for withdrawal $withdrawalDetails is $changeAddress")
            transactionHelper.addOutputs(
                transaction,
                unspents,
                withdrawalDetails.toAddress,
                withdrawalDetails.amountSat,
                Address.fromBase58(
                    btcNetworkConfigProvider.getConfig(),
                    changeAddress
                )
            )
            unspents
        }.map { unspents -> Pair(transaction, unspents) }
    }

    /**
     * Chooses change addresses for withdrawal among set of change addresses
     * @param withdrawalDetails - details of withdrawal
     * @param changeAddresses - all change addresses
     * @return change address
     */
    private fun chooseChangeAddress(
        withdrawalDetails: WithdrawalDetails,
        changeAddresses: List<BtcAddress>
    ): BtcAddress {
        /*
        Every node must choose the same change address in order to create the same Bitcoih transaction.
        Address must be chosen randomly to distribute changes among addresses equally.
        We can do it using Random() that takes withdrawal time as a seed
        (we assume that withdrawal time is the same on all the nodes)
         */
        val random = Random(withdrawalDetails.withdrawalTime)
        return changeAddresses.random(random)
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
