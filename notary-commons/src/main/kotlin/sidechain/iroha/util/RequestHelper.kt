package sidechain.iroha.util

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import iroha.protocol.BlockOuterClass
import iroha.protocol.Commands
import iroha.protocol.QryResponses
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Query
import jp.co.soramitsu.iroha.java.QueryBuilder
import model.IrohaCredential
import java.time.Instant

/**
 * Get asset precision
 *
 * @param iroha - iroha network layer
 * @param irohaCredential - iroha account
 * @param assetId asset id in Iroha
 */
fun getAssetPrecision(
    iroha: IrohaAPI,
    irohaCredential: IrohaCredential,
    assetId: String
): Result<Int, Exception> {
    val query = QueryBuilder(irohaCredential.accountId, Instant.now(), 1)
        .getAssetInfo(assetId)
        .buildSigned(irohaCredential.keyPair)

    return Result.of { iroha.query(query) }
        .map { queryResponse ->
            validateResponse(queryResponse, "asset_response")
            queryResponse.assetResponse.asset.precision
        }
}

/**
 * Get asset balance
 *
 * @param iroha - iroha network layer
 * @param irohaCredential - iroha account
 * @param accountId - iroha account
 *
 * @return asset account balance if asset is found, otherwise "0"
 */
fun getAccountAsset(
    iroha: IrohaAPI,
    irohaCredential: IrohaCredential,
    accountId: String,
    assetId: String
): Result<String, Exception> {
    val query = Query.builder(irohaCredential.accountId, 1)
        .getAccountAssets(accountId)
        .buildSigned(irohaCredential.keyPair)

    return Result.of { iroha.query(query) }
        .map { queryResponse ->
            validateResponse(queryResponse, "account_assets_response")

            val accountAsset = queryResponse.accountAssetsResponse.accountAssetsList
                .find { it.assetId == assetId }

            accountAsset?.balance ?: "0"
        }
}


/**
 * Retrieves account JSON data from Iroha
 * @param iroha - iroha network layer
 * @param irohaCredential - iroha account
 * @param acc account to retrieve relays from
 * @return Map with account details
 */
fun getAccountData(
    iroha: IrohaAPI,
    irohaCredential: IrohaCredential,
    acc: String
): Result<JsonObject, Exception> {
    val query = Query.builder(irohaCredential.accountId, 1)
        .getAccount(acc)
        .buildSigned(irohaCredential.keyPair)

    return Result.of { iroha.query(query) }
        .map { queryResponse ->
            validateResponse(queryResponse, "account_response")
            val account = queryResponse.accountResponse.account
            val stringBuilder = StringBuilder(account.jsonData)
            Parser().parse(stringBuilder) as JsonObject
        }
}

/**
 * Retrieves account details by setter from Iroha
 * @param iroha - iroha network layer
 * @param irohaCredential - iroha account
 * @param acc - account to read details from
 * @param detailSetterAccount - account that has set the details
 * @return Map with account details
 */
fun getAccountDetails(
    iroha: IrohaAPI,
    irohaCredential: IrohaCredential,
    acc: String,
    detailSetterAccount: String
): Result<Map<String, String>, Exception> {
    return getAccountData(
        iroha,
        irohaCredential,
        acc
    ).map { json ->
        if (json.map[detailSetterAccount] == null)
            mapOf()
        else
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
        validateResponse(queryResponse, "transactions_response")
        if (queryResponse.transactionsResponse.transactionsCount == 0)
            throw Exception("There is no transactions.")

        // return transaction
        queryResponse.transactionsResponse.transactionsList[0]
    }
}

/**
 * Return all "set account detail" commands from Iroha block
 * @param block - Iroha block
 * @return list full of "set account detail" commands
 */
fun getSetDetailCommands(block: BlockOuterClass.Block): List<Commands.Command> {
    return block.payload.transactionsList.flatMap { tx ->
        tx.payload.reducedPayload.commandsList
    }.filter { command -> command.hasSetAccountDetail() }
}

/**
 * Return all "transfer asset" commands from Iroha block
 * @param block - Iroha block
 * @return list full of "transfer asset" commands
 */
fun getTransferCommands(block: BlockOuterClass.Block): List<Commands.Command> {
    return block.payload.transactionsList.flatMap { tx -> tx.payload.reducedPayload.commandsList }
        .filter { command -> command.hasTransferAsset() }
}

/**
 * Throws exception, if fieldName is not present
 * @param queryResponse - query response on getTransactions
 * @param fieldName - field name to validate
 */
private fun validateResponse(queryResponse: QryResponses.QueryResponse, fieldName: String) {
    val fieldDescriptor = queryResponse.descriptorForType.findFieldByName(fieldName)
    if (!queryResponse.hasField(fieldDescriptor)) {
        throw IllegalStateException("Query response error: ${queryResponse.errorResponse}")
    }
}
