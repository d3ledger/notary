package registration

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.google.protobuf.InvalidProtocolBufferException
import io.grpc.ManagedChannelBuilder
import iroha.protocol.Queries
import iroha.protocol.QueryServiceGrpc
import jp.co.soramitsu.iroha.Keypair
import jp.co.soramitsu.iroha.ModelProtoQuery
import jp.co.soramitsu.iroha.ModelQueryBuilder
import main.ConfigKeys
import mu.KLogging
import notary.CONFIG
import sidechain.iroha.util.toByteArray
import java.math.BigInteger

/**
 * Provides with free ethereum relay wallet
 */
class EthFreeWalletsProvider(val keypair: Keypair) {

    fun getWallet(): String {
        val creator = CONFIG[ConfigKeys.irohaCreator]
        val startQueryCounter: Long = 1
        val currentTime = System.currentTimeMillis()
        val channel = ManagedChannelBuilder.forAddress(CONFIG[ConfigKeys.irohaHostname], CONFIG[ConfigKeys.irohaPort])
            .usePlaintext(true).build()
        val master = CONFIG[ConfigKeys.irohaMaster]

        // query result of transaction we've just sent
        val uquery = ModelQueryBuilder().creatorAccountId(creator)
            .queryCounter(BigInteger.valueOf(startQueryCounter))
            .createdTime(BigInteger.valueOf(currentTime))
            .getAccount(master)
            .build()
        val queryBlob = ModelProtoQuery(uquery).signAndAddSignature(keypair).finish().blob()
        val bquery = queryBlob.toByteArray()

        val protoQuery: Queries.Query?
        try {
            protoQuery = Queries.Query.parseFrom(bquery)
        } catch (e: InvalidProtocolBufferException) {
            logger.error { "Exception while converting byte array to protobuf: ${e.message}" }
            throw Exception("Exception while converting byte array to protobuf: ${e.message}")
        }


        val queryStub = QueryServiceGrpc.newBlockingStub(channel)
        val queryResponse = queryStub.find(protoQuery)

        val fieldDescriptor = queryResponse.descriptorForType.findFieldByName("account_response")
        if (!queryResponse.hasField(fieldDescriptor)) {
            logger.error { "Query response error" }
            throw Exception("Query response error")
        }

        val account = queryResponse.accountResponse.account
        println("account data ${account.jsonData}")

        val stringBuilder = StringBuilder(account.jsonData)
        val json: JsonObject = Parser().parse(stringBuilder) as JsonObject

        val myMap: Map<String, String> = json.map[master] as Map<String, String>
        val res = myMap.filterValues { it == "free" }.keys

        return res.first()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
