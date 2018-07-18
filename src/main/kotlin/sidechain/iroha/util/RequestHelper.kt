package sidechain.iroha.util

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.google.protobuf.InvalidProtocolBufferException
import config.IrohaConfig
import io.grpc.ManagedChannelBuilder
import iroha.protocol.Queries
import iroha.protocol.QueryServiceGrpc
import jp.co.soramitsu.iroha.ModelProtoQuery
import jp.co.soramitsu.iroha.ModelQueryBuilder
import registration.EthFreeWalletsProvider
import java.math.BigInteger

/**
 * Retrieves relays from Iroha
 * @param acc account to retrieve relays from
 * @param detailSetterAccount - account that has set the details
 * @return Map with relay addresses as keys and iroha accounts (or "free") as values
 */
fun getRelays(irohaConfig: IrohaConfig, acc: String, detailSetterAccount: String): Map<String, String> {
    val creator = irohaConfig.creator

    val keypair = ModelUtil.loadKeypair(
        irohaConfig.pubkeyPath,
        irohaConfig.privkeyPath
    ).get()

    val currentTime = System.currentTimeMillis()

    val uquery = ModelQueryBuilder().creatorAccountId(creator)
        .queryCounter(BigInteger.valueOf(1))
        .createdTime(BigInteger.valueOf(currentTime))
        .getAccount(acc)
        .build()
    val queryBlob = ModelProtoQuery(uquery).signAndAddSignature(keypair).finish().blob()
    val bquery = queryBlob.toByteArray()

    val protoQuery: Queries.Query?
    try {
        protoQuery = Queries.Query.parseFrom(bquery)
    } catch (e: InvalidProtocolBufferException) {
        throw Exception("Exception while converting byte array to protobuf: ${e.message}")
    }

    val channel = ManagedChannelBuilder.forAddress(
        irohaConfig.hostname,
        irohaConfig.port
    )
        .usePlaintext(true).build()
    val queryStub = QueryServiceGrpc.newBlockingStub(channel)
    val queryResponse = queryStub.find(protoQuery)

    val fieldDescriptor = queryResponse.descriptorForType.findFieldByName("account_response")
    if (!queryResponse.hasField(fieldDescriptor)) {
        EthFreeWalletsProvider.logger.error { "Query response error: ${queryResponse.errorResponse}" }
        throw Exception("Query response error: ${queryResponse.errorResponse}")
    }

    val account = queryResponse.accountResponse.account

    val stringBuilder = StringBuilder(account.jsonData)
    val json: JsonObject = Parser().parse(stringBuilder) as JsonObject

    if (json.map[detailSetterAccount] == null)
        throw Exception("No free relay wallets found. There is no attributes set by $detailSetterAccount")
    return json.map[detailSetterAccount] as Map<String, String>
}
