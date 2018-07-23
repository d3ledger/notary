package sidechain.iroha.util

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.IrohaConfig
import iroha.protocol.QryResponses
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.Keypair
import jp.co.soramitsu.iroha.ModelQueryBuilder
import notary.endpoint.eth.NotaryException
import sidechain.iroha.consumer.IrohaNetwork
import java.math.BigInteger

/**
 * Retrieves relays from Iroha
 * @param irohaConfig - Iroha configuration parameters
 * @param keypair - iroha keypair
 * @param irohaNetwork - iroha network layer
 * @param acc account to retrieve relays from
 * @param detailSetterAccount - account that has set the details
 * @return Map with relay addresses as keys and iroha accounts (or "free") as values
 */
fun getRelays(
    irohaConfig: IrohaConfig,
    keypair: Keypair,
    irohaNetwork: IrohaNetwork,
    acc: String,
    detailSetterAccount: String
): Result<Map<String, String>, Exception> {
    val uquery = ModelQueryBuilder().creatorAccountId(irohaConfig.creator)
        .queryCounter(BigInteger.valueOf(1))
        .createdTime(ModelUtil.getCurrentTime())
        .getAccount(acc)
        .build()

    return ModelUtil.prepareQuery(uquery, keypair)
        .flatMap { irohaNetwork.sendQuery(it) }
        .map { queryResponse ->
            val fieldDescriptor = queryResponse.descriptorForType.findFieldByName("account_response")
            if (!queryResponse.hasField(fieldDescriptor)) {
                throw Exception("Query response error: ${queryResponse.errorResponse}")
            }

            val account = queryResponse.accountResponse.account
            val stringBuilder = StringBuilder(account.jsonData)
            val json: JsonObject = Parser().parse(stringBuilder) as JsonObject

            if (json.map[detailSetterAccount] == null)
                throw Exception("No relay wallets found. There is no attributes set by $detailSetterAccount")
            json.map[detailSetterAccount] as Map<String, String>
        }
}

/**
 * Return first transaction from transaction query response
 * @param queryResponse - query response on getTransactions
 * @return first transaction
 */
fun getFirstTransaction(queryResponse: QryResponses.QueryResponse): Result<TransactionOuterClass.Transaction, Exception> {
    return Result.of {
        val fieldDescriptor = queryResponse.descriptorForType.findFieldByName("transactions_response")

        if (!queryResponse.hasField(fieldDescriptor)) {
            throw NotaryException("Query response error ${queryResponse.errorResponse}")
        }

        if (queryResponse.transactionsResponse.transactionsCount == 0)
            throw Exception("There is no transactions.")

        // return transaction
        queryResponse.transactionsResponse.transactionsList[0]
    }
}
