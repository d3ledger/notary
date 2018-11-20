package withdrawalservice

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import model.IrohaCredential
import mu.KLogging
import sidechain.iroha.consumer.IrohaConsumer
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.getAccountDetails
import util.irohaEscape
import java.util.*
import kotlin.collections.set

class WithdrawalTxDAOImpl(
    private val irohaConsumer: IrohaConsumer,
    private val credential: IrohaCredential,
    private val irohaNetwork: IrohaNetwork,
    private val accountId: String
) : WithdrawalTxDAO<String, String> {
    // TODO: make write operations atomic, think about data consistency inside acc details
    private val klaxon = Klaxon()
    private val parser = Parser()

    override fun put(sourceTranscationDescription: String, targetTranscationDescription: String) {
        pullTransactionsMap()
            .map { transactions ->
                transactions[sourceTranscationDescription] = targetTranscationDescription
                pushTransactionsMap(transactions)
            }
    }

    override fun get(sourceTranscationDescription: String): String {
        return pullTransactionsMap().get()[sourceTranscationDescription].toString()
    }

    override fun remove(sourceTranscationDescription: String) {
        pullTransactionsMap()
            .map { transactions ->
                transactions.remove(sourceTranscationDescription)
                pushTransactionsMap(transactions)
            }
    }

    override fun getObservable(): Observable<Map.Entry<String, String>> {
        return pullTransactionsMap().get().entries.toObservable()
    }

    private fun pushTransactionsMap(transactions: Map<String, String>) {
        val value = String.irohaEscape(klaxon.toJsonString(transactions as HashMap<*, *>))
        ModelUtil.setAccountDetail(
            irohaConsumer,
            accountId,
            STORAGE_KEY,
            value
        )
    }

    private fun pullTransactionsMap(): Result<MutableMap<String, String>, Exception> {
        return getAccountDetails(
            credential,
            irohaNetwork,
            accountId,
            irohaConsumer.creator
        ).map { details ->
            Result.of {
                val jsonObject = parser.parse(StringBuilder(details[STORAGE_KEY] ?: "{}")) as JsonObject
                jsonObject.map as MutableMap<String, String>
            }.fold({ map ->
                map
            }, { ex: Exception ->
                logger.error("Withdrawal storage map parsing error", ex)
                hashMapOf()
            })
        }
    }
    
    companion object : KLogging() {
        private const val STORAGE_KEY = "ETH_WITHDRAWALS"
    }
}
