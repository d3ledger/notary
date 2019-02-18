package com.d3.btc.withdrawal.transaction

import com.d3.btc.monitoring.Monitoring
import org.bitcoinj.core.Transaction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/*
    Class that is used to store unsigned transactions
 */

@Component
class UnsignedTransactions(
    @Autowired private val signCollector: SignCollector
) : Monitoring() {
    override fun monitor() = unsignedTransactions.values.map { timedTx -> timedTx.tx.toString() }

    private val unsignedTransactions = ConcurrentHashMap<String, WithdrawalTx>()

    /**
     * Marks transaction as unsigned. Transaction will be added to [unsignedTransactions] collection under the hood
     * @param withdrawalDetails - details of withdrawal(account id, amount and time)
     * @param transaction - transaction to mark as unsigned
     */
    fun markAsUnsigned(withdrawalDetails: WithdrawalDetails, transaction: Transaction) {
        unsignedTransactions[signCollector.shortTxHash(transaction)] = WithdrawalTx(withdrawalDetails, transaction)
    }

    /**
     * Removes transaction from [unsignedTransactions]
     * @param txHash - hash of transaction to be removed
     */
    fun remove(txHash: String) {
        unsignedTransactions.remove(signCollector.shortTxHash(txHash))
    }

    /**
     * Returns unsigned transaction by its hash
     * @param txHash - hash of transaction
     * @return unsigned transaction or null
     */
    fun get(txHash: String) = unsignedTransactions[signCollector.shortTxHash(txHash)]

    /**
     * Checks if transaction with given hash is in [unsignedTransactions] collection
     * @param txHash - hash of transaction to be checked
     * @return - true if transaction with [txHash] is present
     */
    fun isUnsigned(txHash: String) = unsignedTransactions.containsKey(signCollector.shortTxHash(txHash))
}
