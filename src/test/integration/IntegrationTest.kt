package integration

import Keypair
import ModelProtoQuery
import ModelQueryBuilder
import com.github.kittinunf.result.failure
import com.google.protobuf.InvalidProtocolBufferException
import contract.User
import io.grpc.ManagedChannelBuilder
import iroha.protocol.Queries.Query
import iroha.protocol.QueryServiceGrpc
import main.CONFIG
import main.ConfigKeys
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Numeric
import sideChain.iroha.IrohaInitializtion
import sideChain.iroha.consumer.IrohaKeyLoader
import sideChain.iroha.util.toByteArray
import java.math.BigInteger

class IntegrationTest {

    val web3 = Web3j.build(HttpService(CONFIG[ConfigKeys.ethConnectionUrl]))
    val credentials =
        WalletUtils.loadCredentials("user", "deploy/ethereum/keys/user.key")

    /**
     * Send Ethereum transaction to wallet with specified data.
     */
    fun sendEthereum() {
        val myAddress = "0x004ec07d2329997267Ec62b4166639513386F32E"
        val toAddress = "0x00aa39d30f0d20ff03a22ccfc30b7efbfca597c2"
        val gasPrice = BigInteger.valueOf(1)
        val gasLimit = BigInteger.valueOf(999999)
        val value = BigInteger.valueOf(1234567890123456)
        val data = "admin@test"

        // get the next available nonce
        val ethGetTransactionCount = web3.ethGetTransactionCount(
            myAddress, DefaultBlockParameterName.LATEST
        ).send()
        val nonce = ethGetTransactionCount.getTransactionCount()

        // create our transaction
        val rawTransaction = RawTransaction.createTransaction(
            nonce, gasPrice, gasLimit, toAddress, value, Numeric.toHexString(data.toByteArray())
        )

        // sign & send our transaction
        val signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials)
        val hexValue = Numeric.toHexString(signedMessage)
        web3.ethSendRawTransaction(hexValue).send()
    }

    /**
     * Query Iroha account balance
     */
    fun queryIroha() {
        IrohaInitializtion.loadIrohaLibrary()
            .failure {
                println(it)
                System.exit(1)
            }

        val irohaPort = CONFIG[ConfigKeys.irohaPort]
        val irohaHost = CONFIG[ConfigKeys.irohaHostname]

        val queryBuilder = ModelQueryBuilder()
        val protoQueryHelper = ModelProtoQuery()
        val creator = "admin@test"
        val accountId = "admin@test"
        val assetId = "eth#test"
        val startQueryCounter: Long = 1
        val keypair: Keypair =
            IrohaKeyLoader.loadKeypair(CONFIG[ConfigKeys.pubkeyPath], CONFIG[ConfigKeys.privkeyPath]).get()

        val uquery = queryBuilder.creatorAccountId(creator)
            .queryCounter(BigInteger.valueOf(startQueryCounter))
            .createdTime(BigInteger.valueOf(System.currentTimeMillis()))
            .getAccountAssets(accountId, assetId)
            .build()
        val queryBlob = protoQueryHelper.signAndAddSignature(uquery, keypair).blob().toByteArray()

        val protoQuery: Query?
        try {
            protoQuery = Query.parseFrom(queryBlob)
        } catch (e: InvalidProtocolBufferException) {
            fail { "Exception while converting byte array to protobuf:" + e.message }
        }

        val channel = ManagedChannelBuilder.forAddress(irohaHost, irohaPort).usePlaintext(true).build()
        val queryStub = QueryServiceGrpc.newBlockingStub(channel)
        val queryResponse = queryStub.find(protoQuery)

        val fieldDescriptor =
            queryResponse.descriptorForType.findFieldByName("account_assets_response")
        if (!queryResponse.hasField(fieldDescriptor)) {
            fail { "Query response error" }
        }

        val asset = queryResponse.accountAssetsResponse.accountAsset
        println("asset " + asset.assetId)
        println("account " + asset.accountId)
        println("balance " + asset.balance)
    }

    fun deployUserSmartContract() {
        println(credentials.address)
        val contract =
            contract.User.deploy(
                web3,
                credentials,
                BigInteger.valueOf(1),
                BigInteger.valueOf(100000),
                "0x004ec07d2329997267ec62b4166639513386f32e"
            ).send()

        println("User contract address: ${contract.contractAddress}")
    }

    @Test
    fun sendEthereumTest() {
//        sendEthereum()
    }

    @Test
    fun queryIrohaTest() {
//        queryIroha()
    }
}
