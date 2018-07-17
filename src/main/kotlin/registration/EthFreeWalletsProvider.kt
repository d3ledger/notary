package registration

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.google.protobuf.InvalidProtocolBufferException
import config.IrohaConfig
import io.grpc.ManagedChannelBuilder
import iroha.protocol.Queries
import iroha.protocol.QueryServiceGrpc
import jp.co.soramitsu.iroha.Keypair
import jp.co.soramitsu.iroha.ModelProtoQuery
import jp.co.soramitsu.iroha.ModelQueryBuilder
import mu.KLogging
import sidechain.iroha.util.toByteArray
import java.math.BigInteger

/**
 * Provides with free ethereum relay wallet
 * @param keypair - iroha keypair
 * @param notaryIrohaAccount - Master notary account in Iroha to write down the information about free relay wallets has been added
 */
// TODO Prevent double relay accounts usage (in perfect world it is on Iroha side with custom code). In real world
// on provider side with some synchronization.
class EthFreeWalletsProvider(
    irohaConfig: IrohaConfig,
    val keypair: Keypair,
    val notaryIrohaAccount: String,
    val relayRegistrationAccount: String
) {

    /** Creator of txs in Iroha  */
    val creator = irohaConfig.creator

    /** Iroha query counter */
    var queryCounter: Long = 1

    val channel = ManagedChannelBuilder.forAddress(
        irohaConfig.hostname,
        irohaConfig.port
    )
        .usePlaintext(true).build()

    fun getWallet(): String {
        val currentTime = System.currentTimeMillis()

        val uquery = ModelQueryBuilder().creatorAccountId(creator)
            .queryCounter(BigInteger.valueOf(queryCounter))
            .createdTime(BigInteger.valueOf(currentTime))
            .getAccount(notaryIrohaAccount)
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
            logger.error { "Query response error: ${queryResponse.errorResponse}" }
            throw Exception("Query response error: ${queryResponse.errorResponse}")
        }

        val account = queryResponse.accountResponse.account

        val stringBuilder = StringBuilder(account.jsonData)
        val json: JsonObject = Parser().parse(stringBuilder) as JsonObject

        if (json.map[relayRegistrationAccount] == null)
            throw Exception("No free relay wallets found. There is no attributes set by $relayRegistrationAccount")
        val myMap: Map<String, String> = json.map[relayRegistrationAccount] as Map<String, String>
        val res = myMap.filterValues { it == "free" }.keys

        return res.first()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
