package sidechain.iroha.util

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import iroha.protocol.BlockOuterClass
import iroha.protocol.Commands
import iroha.protocol.QryResponses
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.Hash
import jp.co.soramitsu.iroha.HashVector
import jp.co.soramitsu.iroha.ModelQueryBuilder
import model.IrohaCredential
import sidechain.iroha.consumer.IrohaNetwork
import java.math.BigInteger

/**
 * Get asset precision
 *
 * @param credential - iroha credential
 * @param irohaNetwork - iroha network layer
 * @param assetId asset id in Iroha
 */
fun getAssetPrecision(
    credential: IrohaCredential,
    irohaNetwork: IrohaNetwork,
    assetId: String
): Result<Short, Exception> {
    val uquery = ModelQueryBuilder().creatorAccountId(credential.accountId)
        .queryCounter(BigInteger.valueOf(1))
        .createdTime(ModelUtil.getCurrentTime())
        .getAssetInfo(assetId)
        .build()

    return ModelUtil.prepareQuery(uquery, credential.keyPair)
        .flatMap { query -> irohaNetwork.sendQuery(query) }
        .map { queryResponse ->
            validateResponse(queryResponse, "asset_response")
            queryResponse.assetResponse.asset.precision.toShort()
        }
}


/**
 * Get asset info
 *
 * @param credential - iroha credential
 * @param irohaNetwork - iroha network layer
 * @param assetId asset id in Iroha
 */
fun getAssetInfo(
    credential: IrohaCredential,
    irohaNetwork: IrohaNetwork,
    assetId: String
): Result<QryResponses.Asset, Exception> {
    val uquery = ModelQueryBuilder().creatorAccountId(credential.accountId)
        .queryCounter(BigInteger.valueOf(1))
        .createdTime(ModelUtil.getCurrentTime())
        .getAssetInfo(assetId)
        .build()

    return ModelUtil.prepareQuery(uquery, credential.keyPair)
        .flatMap { query -> irohaNetwork.sendQuery(query) }
        .map { queryResponse ->
            validateResponse(queryResponse, "asset_response")
            queryResponse.assetResponse.asset
        }
}

/**
 * Get all account assets
 *
 * @param credential - iroha credential
 * @param irohaNetwork - iroha network layer
 * @param accountId - iroha account
 * @return Map (assetId -> balance)
 * */
fun getAccountAssets(
    credential: IrohaCredential,
    irohaNetwork: IrohaNetwork,
    accountId: String
): Result<Map<String, String>, Exception> {
    val uquery = ModelQueryBuilder()
        .creatorAccountId(credential.accountId)
        .queryCounter(BigInteger.valueOf(1))
        .createdTime(ModelUtil.getCurrentTime())
        .getAccountAssets(accountId)
        .build()

    return ModelUtil.prepareQuery(uquery, credential.keyPair)
        .flatMap { query -> irohaNetwork.sendQuery(query) }
        .map { queryResponse ->
            validateResponse(queryResponse, "account_assets_response")

            queryResponse.accountAssetsResponse.accountAssetsList
                .map {
                    it.assetId to it.balance
                }.toMap()
        }
}

/**
 * Get asset balance
 *
 * @param credential - iroha credential
 * @param irohaNetwork - iroha network layer
 * @param accountId - iroha account
 *
 * @return asset account balance if asset is found, otherwise "0"
 */
fun getAccountAssetBalance(
    credential: IrohaCredential,
    irohaNetwork: IrohaNetwork,
    accountId: String,
    assetId: String
): Result<String, Exception> {
    return getAccountAssets(credential, irohaNetwork, accountId)
        .map { it.getOrDefault(assetId, "0") }
}

/**
 * Retrieves account JSON data from Iroha
 * @param credential - iroha credential
 * @param irohaNetwork - iroha network layer
 * @param acc account to retrieve relays from
 * @return Map with account details
 */
fun getAccountData(
    credential: IrohaCredential,
    irohaNetwork: IrohaNetwork,
    acc: String
): Result<JsonObject, Exception> {
    val uquery = ModelQueryBuilder().creatorAccountId(credential.accountId)
        .queryCounter(BigInteger.valueOf(1))
        .createdTime(ModelUtil.getCurrentTime())
        .getAccount(acc)
        .build()

    return ModelUtil.prepareQuery(uquery, credential.keyPair)
        .flatMap { query -> irohaNetwork.sendQuery(query) }
        .map { queryResponse ->
            validateResponse(queryResponse, "account_response")
            val account = queryResponse.accountResponse.account
            val stringBuilder = StringBuilder(account.jsonData)
            Parser().parse(stringBuilder) as JsonObject
        }
}

/**
 * Retrieves account details by setter from Iroha
 * @param credential - iroha credential
 * @param irohaNetwork - iroha network layer
 * @param acc - account to read details from
 * @param detailSetterAccount - account that has set the details
 * @return Map with account details
 */
fun getAccountDetails(
    credential: IrohaCredential,
    irohaNetwork: IrohaNetwork,
    acc: String,
    detailSetterAccount: String
): Result<MutableMap<String, String>, Exception> {
    return getAccountData(
        credential,
        irohaNetwork,
        acc
    ).map { json ->
        if (json.map[detailSetterAccount] == null)
            mutableMapOf()
        else
            json.map[detailSetterAccount] as MutableMap<String, String>
    }
}

/**
 * Get transaction from Iroha by [hash]
 * @param hash - hash of transaction
 * @return transaction
 */
fun getTransaction(
    irohaNetwork: IrohaNetwork,
    credential: IrohaCredential,
    hash: String
): Result<TransactionOuterClass.Transaction, Exception> {
    val hashes = HashVector()
    hashes.add(Hash.fromHexString(hash))

    val uquery = ModelQueryBuilder().creatorAccountId(credential.accountId)
        .queryCounter(BigInteger.valueOf(1))
        .createdTime(BigInteger.valueOf(System.currentTimeMillis()))
        .getTransactions(hashes)
        .build()

    return ModelUtil.prepareQuery(uquery, credential.keyPair)
        .flatMap { irohaNetwork.sendQuery(it) }
        .flatMap { getFirstTransaction(it) }
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
