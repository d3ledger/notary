package withdrawalservice

import com.beust.klaxon.Klaxon
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import model.IrohaCredential
import org.web3j.protocol.core.methods.response.TransactionReceipt
import sidechain.iroha.consumer.IrohaConsumer
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.getAccountDetails
import kotlin.collections.set

class WithdrawalTxDAOImpl(
    private val irohaConsumer: IrohaConsumer,
    private val credential: IrohaCredential,
    private val irohaNetwork: IrohaNetwork,
    private val accountId: String

) : WithdrawalTxDAO<String, TransactionReceipt?> {
    // TODO: make write operations atomic, think about data consistency inside acc details
    // TODO: think about removal from txMap
    private val klaxon = Klaxon()
    private val txMap = hashMapOf<String, TransactionReceipt?>()

    init {
        launch {
            delay(delayMs)
            txMap.putAll(pullTransactionsMap().get())
        }
    }

    override fun store(sourceTranscationDescription: String, targetTranscationDescription: TransactionReceipt?) {
        getMutableTransactions()
            .map { transactions ->
                transactions[sourceTranscationDescription] = targetTranscationDescription
                pushTransactionsMap(transactions)
            }
    }

    override fun getTarget(sourceTranscationDescription: String): TransactionReceipt? {
        return pullTransactionsMap().get()[sourceTranscationDescription]
    }

    override fun remove(sourceTranscationDescription: String) {
        getMutableTransactions()
            .map { transactions ->
                transactions.remove(sourceTranscationDescription)
                pushTransactionsMap(transactions)
            }
    }

    override fun getObservable(): Observable<Map.Entry<String, TransactionReceipt?>> {
        return txMap.entries.toObservable()
    }

    private fun pushTransactionsMap(transactions: Map<String, TransactionReceipt?>) {
        ModelUtil.setAccountDetail(
            irohaConsumer,
            accountId,
            STORAGE_KEY,
            klaxon.toJsonString(transactions)
        )
    }

    private fun pullTransactionsMap(): Result<Map<String, TransactionReceipt?>, Exception> {
        return getAccountDetails(
            credential,
            irohaNetwork,
            accountId,
            accountId
        ).map { details ->
            details[STORAGE_KEY].let { mapInString ->
                mapInString?.let {
                    klaxon.parse<MutableMap<String, TransactionReceipt?>>(it)
                }!!
            }
        }
    }

    private fun getMutableTransactions(): Result<MutableMap<String, TransactionReceipt?>, Exception> {
        return pullTransactionsMap()
            .map {
                it.toMutableMap()
            }
    }

    companion object {
        private const val STORAGE_KEY = "ETH_WITHDRAWALS"
        private const val delayMs = 2000L
    }
}
