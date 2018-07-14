package util.iroha

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.google.protobuf.InvalidProtocolBufferException
import config.ConfigKeys
import io.grpc.ManagedChannelBuilder
import iroha.protocol.Queries
import iroha.protocol.QueryServiceGrpc
import jp.co.soramitsu.iroha.Keypair
import jp.co.soramitsu.iroha.ModelProtoQuery
import jp.co.soramitsu.iroha.ModelQueryBuilder
import notary.CONFIG
import registration.EthFreeWalletsProvider
import sidechain.iroha.util.toByteArray
import java.math.BigInteger

/**
 *
 */
fun getRelays(acc: String, keypair: Keypair): Map<String, String> {
    val creator = CONFIG[ConfigKeys.registrationServiceIrohaAccount]
    val relayRegistrationAccount = CONFIG[ConfigKeys.registrationServiceRelayRegistrationIrohaAccount]

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
            CONFIG[ConfigKeys.registrationServiceIrohaHostname],
            CONFIG[ConfigKeys.registrationServiceIrohaPort])
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

    if (json.map[relayRegistrationAccount] == null)
        throw Exception("No free relay wallets found. There is no attributes set by $relayRegistrationAccount")
    return json.map[relayRegistrationAccount] as Map<String, String>
}
