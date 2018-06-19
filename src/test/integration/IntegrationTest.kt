package integration

import com.github.kittinunf.result.failure
import com.google.protobuf.InvalidProtocolBufferException
import io.grpc.ManagedChannelBuilder
import iroha.protocol.BlockOuterClass
import iroha.protocol.CommandServiceGrpc
import iroha.protocol.Queries.Query
import iroha.protocol.QueryServiceGrpc
import jp.co.soramitsu.iroha.*
import kotlinx.coroutines.experimental.async
import main.CONFIG
import main.ConfigKeys
import main.main
import org.junit.jupiter.api.Disabled
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


/**
 * Class for Ethereum sidechain infrastructure deployment and communication.
 */
class IntegrationTest {

    init {
        IrohaInitializtion.loadIrohaLibrary()
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
     */
    fun sendTxToIroha(txBuilder: ModelTransactionBuilder) {
        val utx = txBuilder.build()

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
    }

    /**
     * Helper function.
     * Add 100420500 ethereum wei to admin@notary
     */
    @Disabled
    @Test
    fun addAssetIroha() {
        val currentTime = System.currentTimeMillis()
        val srcAccountId = "admin@notary"
        val assetId = "ether#ethereum"
        val amount = "100420500"

        // build transaction (still unsigned)
        val txBuilder = ModelTransactionBuilder().creatorAccountId(creator)
            .createdTime(BigInteger.valueOf(currentTime))
            .addAssetQuantity(srcAccountId, assetId, amount)
        sendTxToIroha(txBuilder)
    }

    /**
     * Helper function.
     * Transfer 420500 ethereum wei from admin@notary to user1@notary
     */
    @Disabled
    @Test
    fun transferAssetIroha() {
        val currentTime = System.currentTimeMillis()
        val srcAccountId = "admin@notary"
        val destAccountId = "user1@notary"
        val assetId = "ether#ethereum"
        val amount = "420500"

        // build transaction (still unsigned)
        val txBuilder = ModelTransactionBuilder().creatorAccountId(creator)
            .createdTime(BigInteger.valueOf(currentTime))
            .transferAsset(srcAccountId, destAccountId, assetId, "description", amount)
        sendTxToIroha(txBuilder)
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

}
