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
 * @param keypair - iroha keypair
 * @param master - Master notary account in Iroha to write down the information about free relay wallets has been added
 */
// TODO Prevent double relay accounts usage (in perfect world it is on Iroha side with custom code). In real world
// on provider side with some synchronization.
class EthFreeWalletsProvider(
    val keypair: Keypair,
    val master: String = CONFIG[ConfigKeys.irohaMaster]
) {

    /** Creator of txs in Iroha  */
    val creator = CONFIG[ConfigKeys.irohaCreator]

    /** Registration service account is needed to get free relay */
    val registrationServiceAccount = CONFIG[ConfigKeys.irohaCreator]

    /** Iroha query counter */
    var queryCounter: Long = 1

    val channel = ManagedChannelBuilder.forAddress(CONFIG[ConfigKeys.irohaHostname], CONFIG[ConfigKeys.irohaPort])
        .usePlaintext(true).build()

    fun getWallet(): String {
        val currentTime = System.currentTimeMillis()

        // query result of transaction we've just sent
        val uquery = ModelQueryBuilder().creatorAccountId(creator)
            .queryCounter(BigInteger.valueOf(queryCounter))
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
        queryCounter++

        val fieldDescriptor = queryResponse.descriptorForType.findFieldByName("account_response")
        if (!queryResponse.hasField(fieldDescriptor)) {
            logger.error { "Query response error" }
            throw Exception("Query response error")
        }

        val account = queryResponse.accountResponse.account

        val stringBuilder = StringBuilder(account.jsonData)
        val json: JsonObject = Parser().parse(stringBuilder) as JsonObject

        if (json.map[registrationServiceAccount] == null)
            throw Exception("There is no attributes set by $registrationServiceAccount")
        val myMap: Map<String, String> = json.map[registrationServiceAccount] as Map<String, String>
        val res = myMap.filterValues { it == "free" }.keys

        return res.first()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
