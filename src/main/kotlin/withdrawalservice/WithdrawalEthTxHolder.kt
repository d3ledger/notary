package withdrawalservice

import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import org.web3j.protocol.core.methods.response.TransactionReceipt

object WithdrawalEthTxHolder : WithdrawalTxHolder<String, TransactionReceipt?> {

    private val transactions: MutableMap<String, TransactionReceipt?> = hashMapOf()

    override fun store(sourceTranscationDescription: String, targetTranscationDescription: TransactionReceipt?) {
        // not sure if observable handles updates
        if(transactions.containsKey(sourceTranscationDescription)) {
            removeEntry(sourceTranscationDescription)
        }
        transactions[sourceTranscationDescription] = targetTranscationDescription
    }

    override fun getTarget(sourceTranscationDescription: String): TransactionReceipt? {
        return transactions[sourceTranscationDescription]
    }

    override fun removeEntry(sourceTranscationDescription: String) {
        transactions.remove(sourceTranscationDescription)
    }

    override fun getObservable(): Observable<Map.Entry<String, TransactionReceipt?>> {
        return transactions.entries.toObservable()
    }
}
