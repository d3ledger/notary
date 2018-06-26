package integration

import com.github.kittinunf.result.failure
import com.google.protobuf.InvalidProtocolBufferException
import com.squareup.moshi.Moshi
import endpoint.eth.BigIntegerMoshiAdapter
import endpoint.eth.EthNotaryResponse
import endpoint.eth.EthNotaryResponseMoshiAdapter
import io.grpc.ManagedChannelBuilder
import iroha.protocol.BlockOuterClass
import iroha.protocol.CommandServiceGrpc
import iroha.protocol.Queries.Query
import iroha.protocol.QueryServiceGrpc
import jp.co.soramitsu.iroha.*
import kotlinx.coroutines.experimental.async
import notary.CONFIG
import main.ConfigKeys
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import notary.main
import org.junit.jupiter.api.fail
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Numeric
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaKeyLoader
import sidechain.iroha.util.toByteArray
import org.junit.jupiter.api.Assertions.assertEquals
import java.math.BigInteger


/**
 * Class for Ethereum sidechain infrastructure deployment and communication.
 */
class IntegrationTest {

    init {
        IrohaInitialization.loadIrohaLibrary()
            .failure {
                println(it)
                System.exit(1)
            }
    }

    /** Iroha keypair */
    val keypair: Keypair =
        IrohaKeyLoader.loadKeypair(CONFIG[ConfigKeys.pubkeyPath], CONFIG[ConfigKeys.privkeyPath]).get()

    /** Iroha host */
    val irohaHost = CONFIG[ConfigKeys.irohaHostname]

    /** Iroha port */
    val irohaPort = CONFIG[ConfigKeys.irohaPort]

    /** Iroha transaction creator */
    val creator = CONFIG[ConfigKeys.irohaCreator]

    /** web3 service instance to communicate with Ethereum network */
    private val web3 = Web3j.build(HttpService(CONFIG[ConfigKeys.ethConnectionUrl]))

    /** credentials of ethereum user */
    private val credentials =
        WalletUtils.loadCredentials("user", "deploy/ethereum/keys/user.key")

    /** Gas price */
    private val gasPrice = BigInteger.ONE

    /** Max gas limit */
    private val gasLimit = BigInteger.valueOf(999999)

    /** Ethereum address to transfer from */
    private val fromAddress = "0x004ec07d2329997267Ec62b4166639513386F32E"

    /** Ethereum address to transfer to */
    private val toAddress = "0x00aa39d30f0d20ff03a22ccfc30b7efbfca597c2"

    /**
     * Send Ethereum transaction to wallet with specified data.
     */
    private fun sendEthereum(amount: BigInteger) {
        // get the next available nonce
        val ethGetTransactionCount = web3.ethGetTransactionCount(
            fromAddress, DefaultBlockParameterName.LATEST
        ).send()
        val nonce = ethGetTransactionCount.transactionCount

        // create our transaction
        val rawTransaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, toAddress, amount, "")

        // sign & send our transaction
        val signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials)
        val hexValue = Numeric.toHexString(signedMessage)
        web3.ethSendRawTransaction(hexValue).send()
    }

    /**
     * Sends transaction to Iroha.
     * @return hex representation of transaction
     */
    fun sendTxToIroha(txBuilder: ModelTransactionBuilder): String {
        val utx = txBuilder.build()
        val hash = utx.hash().hex()

        // sign transaction and get its binary representation (Blob)
        val txblob = ModelProtoTransaction(utx).signAndAddSignature(keypair).finish().blob().toByteArray()

        // create proto object
        var protoTx: BlockOuterClass.Transaction? = null
        try {
            protoTx = BlockOuterClass.Transaction.parseFrom(txblob)
        } catch (e: InvalidProtocolBufferException) {
            System.err.println("Exception while converting byte array to protobuf:" + e.message)
            System.exit(1)
        }

        // Send transaction to iroha
        val channel = ManagedChannelBuilder.forAddress(irohaHost, irohaPort).usePlaintext(true).build()
        val stub = CommandServiceGrpc.newBlockingStub(channel)
        stub.torii(protoTx)

        return hash
    }

    /**
     * Add asset in iroha
     * @return hex representation of transaction hash
     */
    fun addAssetIroha(accountId: String, assetId: String, amount: String): String {
        val currentTime = System.currentTimeMillis()

        // build transaction (still unsigned)
        val txBuilder = ModelTransactionBuilder().creatorAccountId(creator)
            .createdTime(BigInteger.valueOf(currentTime))
            .addAssetQuantity(accountId, assetId, amount)
        return sendTxToIroha(txBuilder)
    }

    /**
     * Transfer asset in iroha
     * @return hex representation of transaction hash
     */
    fun transferAssetIroha(
        srcAccountId: String,
        destAccountId: String,
        assetId: String,
        amount: String,
        description: String
    ): String {
        val currentTime = System.currentTimeMillis()

        // build transaction (still unsigned)
        val txBuilder = ModelTransactionBuilder().creatorAccountId(creator)
            .createdTime(BigInteger.valueOf(currentTime))
            .transferAsset(srcAccountId, destAccountId, assetId, description, amount)
        return sendTxToIroha(txBuilder)
    }

    /**
     * Query Iroha account balance
     */
    fun queryIroha() {
        val accountId = "user1@notary"
        val startQueryCounter: Long = 1

        val uquery = ModelQueryBuilder().creatorAccountId(creator)
            .queryCounter(BigInteger.valueOf(startQueryCounter))
            .createdTime(BigInteger.valueOf(System.currentTimeMillis()))
            .getAccountAssets(accountId)
            .build()
        val queryBlob = ModelProtoQuery(uquery).signAndAddSignature(keypair).finish().blob().toByteArray()

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
            fail { "Query response error ${queryResponse.errorResponse}" }
        }

        val assets = queryResponse.accountAssetsResponse.accountAssetsList
        for (asset in assets) {
            println("account " + asset.accountId)
            println("asset ${asset.assetId} - ${asset.balance}")
        }
    }


    /**
     * Deploy BasicCoin smart contract
     * @return token smart contract address
     */
    private fun deployBasicCoinSmartContract(): String {
        val contract =
            contract.BasicCoin.deploy(
                web3,
                credentials,
                gasPrice,
                gasLimit
            ).send()

        return contract.contractAddress
    }

    /**
     * Deploy notary smart contract
     * @return notary smart contract address
     */
    private fun deployNotarySmartContract(): String {
        val contract =
            contract.Notary.deploy(
                web3,
                credentials,
                gasPrice,
                gasLimit
            ).send()

        return contract.contractAddress
    }

    /**
     * Deploy user smart contract
     * @param master notary master account
     * @param tokens list of supported tokens
     * @return user smart contract address
     */
    private fun deployUserSmartContract(master: String, tokens: List<String>): String {
        val contract =
            contract.User.deploy(
                web3,
                credentials,
                gasPrice,
                gasLimit,
                master,
                tokens
            ).send()

        return contract.contractAddress
    }

    /**
     * Deploy all smart contracts:
     * - notary
     * - token
     * - user
     */
    fun deployAll() {
        val token = deployBasicCoinSmartContract()
        val notary = deployNotarySmartContract()

        val tokens = listOf(token)
        val user = deployUserSmartContract(notary, tokens)

        println("Token contract address: $token")
        println("Notary contract address: $notary")
        println("User contract address: $user")
    }

    /**
     * Test US transfer Ethereum.
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and two ethereum wallets and "fromAddress" with at least 0.001 Ether
     * (1234000000000000 Wei) and notary running
     * @when "fromAddress" transfers 1234000000000000 Wei to "toAddress"
     * @then Associated Iroha account balance is increased on 1234000000000000 Wei
     */
    @Disabled
    @Test
    fun runMain() {
        val amount = BigInteger.valueOf(1_234_000_000_000_000)
        async {
            main(arrayOf())
        }

        Thread.sleep(3_000)
        println("send")
        sendEthereum(amount)
        Thread.sleep(5_000)
        println("send again")
        sendEthereum(amount)
        Thread.sleep(20_000)
        println("query")
        queryIroha()
        println("done")
    }

    /**
     * Test US withdraw Ethereum.
     * Note: Iroha must be deployed to pass the test.
     * @given Iroha networks running and user has sent 64203 Wei to master
     * @when withdrawal service queries notary
     * @then notary replyes with refund information and signature
     */
    @Disabled
    @Test
    fun testRefund() {
        async {
            main(arrayOf())
        }

        val masterAccount = "user1@notary"
        val user = "admin@notary"
        val amount = "64203"
        val assetId = "ether#ethereum"
        val ethWallet = "eth_wallet"

        // add assets to user1@notary
        addAssetIroha(user, assetId, amount)

        // transfer assets from user1@notary to admin@notary
        val hash = transferAssetIroha(user, masterAccount, assetId, amount, ethWallet)

        // query
        Thread.sleep(4_000)
        println("send")
        val res = khttp.get("http://127.0.0.1:8080/eth/$hash")

        val moshi = Moshi
            .Builder()
            .add(EthNotaryResponseMoshiAdapter())
            .add(BigInteger::class.java, BigIntegerMoshiAdapter())
            .build()!!
        val ethNotaryAdapter = moshi.adapter(EthNotaryResponse::class.java)!!
        val response = ethNotaryAdapter.fromJson(res.jsonObject.toString())

        assert(response is EthNotaryResponse.Successful)
        response as EthNotaryResponse.Successful

        assertEquals(BigInteger(amount), response.ethRefund.amount)
        assertEquals(ethWallet, response.ethRefund.address)
        assertEquals("ether", response.ethRefund.assetId)

        assertEquals("mockSignature", response.ethSignature)
    }

}
