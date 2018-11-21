package withdrawalservice

import com.beust.klaxon.Klaxon
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import model.IrohaCredential
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
) : WithdrawalTxDAO<String, String?> {
    // TODO: make write operations atomic, think about data consistency inside acc details
    private val klaxon = Klaxon()
    private val txMap = hashMapOf<String, String?>()

    init {
        launch {
            delay(UPDATE_DELAY)
            txMap.putAll(pullTransactionsMap().get())
        }
    }

    override fun store(sourceTranscationDescription: String, targetTranscationDescription: String?) {
        getMutableTransactions()
            .map { transactions ->
                transactions[sourceTranscationDescription] = targetTranscationDescription
                pushTransactionsMap(transactions)
            }
    }

    override fun getTarget(sourceTranscationDescription: String): String? {
        return pullTransactionsMap().get()[sourceTranscationDescription]
    }

    override fun remove(sourceTranscationDescription: String) {
        txMap.remove(sourceTranscationDescription)
        getMutableTransactions()
            .map { transactions ->
                transactions.remove(sourceTranscationDescription)
                pushTransactionsMap(transactions)
            }
    }

    override fun getObservable(): Observable<Map.Entry<String, String?>> {
        return txMap.entries.toObservable()
    }

    private fun pushTransactionsMap(transactions: Map<String, String?>) {
        ModelUtil.setAccountDetail(
            irohaConsumer,
            accountId,
            STORAGE_KEY,
            klaxon.toJsonString(transactions)
        )
    }

    private fun pullTransactionsMap(): Result<Map<String, String?>, Exception> {
        return getAccountDetails(
            credential,
            irohaNetwork,
            accountId,
            accountId
        ).map { details ->
            details[STORAGE_KEY].let { mapInString ->
                mapInString?.let {
                    klaxon.parse<MutableMap<String, String?>>(it)
                }!!
            }
        }
    }

    private fun getMutableTransactions(): Result<MutableMap<String, String?>, Exception> {
        return pullTransactionsMap()
            .map {
                it.toMutableMap()
            }
    }

    companion object {
        private const val STORAGE_KEY = "ETH_WITHDRAWALS"
        private const val UPDATE_DELAY = 2000L
    }
}
