package withdrawalservice

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.google.protobuf.InvalidProtocolBufferException
import config.ConfigKeys
import io.grpc.ManagedChannelBuilder
import io.reactivex.Observable
import iroha.protocol.Queries
import iroha.protocol.QueryServiceGrpc
import jp.co.soramitsu.iroha.ModelProtoQuery
import jp.co.soramitsu.iroha.ModelQueryBuilder
import notary.CONFIG
import org.web3j.utils.Numeric.hexStringToByteArray
import registration.EthFreeWalletsProvider
import sidechain.SideChainEvent
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.toByteArray
import util.eth.hashToWithdraw
import util.eth.signUserData
import java.math.BigInteger

/**
 * Approval to be passed to the Ethereum for refund
 */
data class RollbackApproval(
        val tokenContractAddress: String,
        val amount: BigInteger,
        val account: String,
        val iroha_hash: String,
        val r: ArrayList<ByteArray>,
        val s: ArrayList<ByteArray>,
        val v: ArrayList<BigInteger>,
        val relay: String)


/**
 * Implementation of Withdrawal Service
 */
class WithdrawalServiceImpl(
        private val irohaHandler: Observable<SideChainEvent.IrohaEvent>
) : WithdrawalService {
    val notaryPeerListProvider = NotaryPeerListProviderImpl()

    val coins: HashMap<String, String> = hashMapOf("ether#ethereum" to "0x0000000000000000000000000000000000000000")

    /** Iroha host */
    val irohaHost = CONFIG[ConfigKeys.testIrohaHostname]

    /** Iroha port */
    val irohaPort = CONFIG[ConfigKeys.testIrohaPort]

    /** Iroha transaction creator */
    val creator = CONFIG[ConfigKeys.testIrohaAccount]

    /** Iroha keypair */
    val keypair =
            ModelUtil.loadKeypair(CONFIG[ConfigKeys.testPubkeyPath], CONFIG[ConfigKeys.testPrivkeyPath]).get()

    val relayRegistrationAccount = CONFIG[ConfigKeys.registrationServiceRelayRegistrationIrohaAccount]

    val masterAccount = CONFIG[ConfigKeys.notaryIrohaAccount]

    fun findInAccDetail(acc: String, name: String): String {
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

        val channel = ManagedChannelBuilder.forAddress(irohaHost, irohaPort).usePlaintext(true).build()
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
        val myMap: Map<String, String> = json.map[relayRegistrationAccount] as Map<String, String>

        for (record in myMap) {
            if (record.value == name) {
                return record.key
            }
        }
        return ""
    }

    /**
     * Query all notaries for approval of refund
     */
    private fun requestNotary(event: SideChainEvent.IrohaEvent.SideChainTransfer): RollbackApproval? {
        // TODO query each notary service and if majority is achieved, send tx to Ethereum SC
        val hash = event.hash
        val amount = event.cmd.amount
        if (!coins.containsKey(event.cmd.assetId)) {
            return null
        }
        val coin = coins[event.cmd.assetId]
        val address = event.cmd.description
        // description field holds target account address
        val relay_address = findInAccDetail(masterAccount, event.cmd.srcAccountId)
        if (relay_address == "") {
            return null
        }
        println("relay found: $relay_address")

        val vv = ArrayList<BigInteger>()
        val rr = ArrayList<ByteArray>()
        val ss = ArrayList<ByteArray>()

        notaryPeerListProvider.getPeerList().forEach { peer ->
            // TODO: replace with valid peer requests
            val signature = signUserData(hashToWithdraw(coin!!, amount, address, hash))
            val r = hexStringToByteArray(signature.substring(2, 66))
            val s = hexStringToByteArray(signature.substring(66, 130))
            val v = signature.substring(130, 132).toBigInteger(16)

            vv.add(v)
            rr.add(r)
            ss.add(s)
        }

        return RollbackApproval(coin!!, amount, address, hash, rr, ss, vv, relay_address)
    }


    /**
     * Handle IrohaEvent
     */
    override fun onIrohaEvent(irohaEvent: SideChainEvent.IrohaEvent): WithdrawalServiceOutputEvent {
        // TODO: use result or exceptions instead of nulls
        var proof: RollbackApproval? = null
        when (irohaEvent) {
            is SideChainEvent.IrohaEvent.SideChainTransfer -> {
                if (irohaEvent.cmd.destAccountId == CONFIG[ConfigKeys.notaryIrohaAccount]) {
                    proof = requestNotary(irohaEvent)
                }
            }
            else -> {}
        }
        return WithdrawalServiceOutputEvent.EthRefund(proof)
    }

    /**
     * Relay events to consumer
     */
    override fun output(): Observable<WithdrawalServiceOutputEvent> {
        return irohaHandler
                .map { onIrohaEvent(it) }
    }

}
