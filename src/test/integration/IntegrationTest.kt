package integration

import com.github.kittinunf.result.failure
import com.google.protobuf.InvalidProtocolBufferException
import com.squareup.moshi.Moshi
import config.loadConfigs
import io.grpc.ManagedChannelBuilder
import iroha.protocol.Queries.Query
import iroha.protocol.QueryServiceGrpc
import jp.co.soramitsu.iroha.*
import kotlinx.coroutines.experimental.async
import notary.db.tables.Tokens
import notary.endpoint.eth.BigIntegerMoshiAdapter
import notary.endpoint.eth.EthNotaryResponse
import notary.endpoint.eth.EthNotaryResponseMoshiAdapter
import notary.main
import org.jooq.impl.DSL
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.web3j.protocol.core.DefaultBlockParameterName
import sidechain.eth.util.DeployHelper
import sidechain.eth.util.hashToWithdraw
import sidechain.eth.util.signUserData
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.toBigInteger
import sidechain.iroha.util.toByteArray
import java.math.BigInteger
import java.sql.DriverManager
import java.util.*

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

    val testConfig = loadConfigs("test", TestConfig::class.java)

    private val deployHelper = DeployHelper(testConfig.ethereum)

    /** Iroha host */
    val irohaHost = testConfig.iroha.hostname

    /** Iroha port */
    val irohaPort = testConfig.iroha.port

    /** Iroha transaction creator */
    val creator = testConfig.iroha.creator

    /** Iroha keypair */
    val keypair = ModelUtil.loadKeypair(testConfig.iroha.pubkeyPath, testConfig.iroha.privkeyPath).get()

    val irohaNetwork = IrohaNetworkImpl(irohaHost, irohaPort)

    val masterAccount = testConfig.notaryIrohaAccount

    /** Ethereum address to transfer to */
    private val toAddress = "0x00aa39d30f0d20ff03a22ccfc30b7efbfca597c2"

    private fun getRandomString(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz"
        val res = StringBuilder()
        for (i in 0..9) {
            res.append(chars[Random().nextInt(chars.length)])
        }
        return res.toString()
    }

    /**
     * Sends transaction to Iroha.
     * @return hex representation of transaction
     */
    fun sendTxToIroha(txBuilder: ModelTransactionBuilder, kp: Keypair = keypair): String {
        val utx = txBuilder.build()
        val hash = utx.hash()

        val tx = ModelUtil.prepareTransaction(utx, kp)
        irohaNetwork.sendAndCheck(tx, hash)

        return hash.hex()
    }

    /**
     * Add asset in iroha
     * @return hex representation of transaction hash
     */
    fun addAssetIroha(assetId: String, amount: String): String {
        val currentTime = System.currentTimeMillis()

        // build transaction (still unsigned)
        val txBuilder = ModelTransactionBuilder().creatorAccountId(creator)
            .createdTime(BigInteger.valueOf(currentTime))
            .addAssetQuantity(assetId, amount)
        return sendTxToIroha(txBuilder)
    }

    /**
     * Set account detail in Iroha
     * @return hex representation of transaction hash
     */
    fun setAccountDetail(accountId: String, key: String, value: String): String {
        val creator = testConfig.iroha.creator
        val currentTime = System.currentTimeMillis()

        // build transaction (still unsigned)
        val txBuilder = ModelTransactionBuilder().creatorAccountId(creator)
            .createdTime(BigInteger.valueOf(currentTime))
            .setAccountDetail(accountId, key, value)
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
        description: String,
        creatorAccountId: String = creator,
        kp: Keypair = keypair
    ): String {
        val currentTime = System.currentTimeMillis()

        // build transaction (still unsigned)
        val txBuilder = ModelTransactionBuilder().creatorAccountId(creatorAccountId)
            .createdTime(BigInteger.valueOf(currentTime))
            .transferAsset(srcAccountId, destAccountId, assetId, description, amount)
        return sendTxToIroha(txBuilder, kp)
    }

    /**
     * Query Iroha account balance
     */
    fun queryIroha(assetId: String, accountId: String = "user1@notary"): BigInteger {
        val queryCounter: Long = 1

        val uquery = ModelQueryBuilder().creatorAccountId(creator)
            .queryCounter(BigInteger.valueOf(queryCounter))
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

        val fieldDescriptor = queryResponse.descriptorForType.findFieldByName("account_assets_response")
        if (!queryResponse.hasField(fieldDescriptor)) {
            fail { "Query response error ${queryResponse.errorResponse}" }
        }

        val assets = queryResponse.accountAssetsResponse.accountAssetsList
        for (asset in assets) {
            if (assetId == asset.assetId)
                return asset.balance.value.toBigInteger()
        }

        return BigInteger.ZERO
    }

    /**
     * Insert token into database
     * @param wallet - ethereum token wallet
     * @param token - token name
     */
    fun insertToken(wallet: String, token: String) {
        val connection = DriverManager.getConnection(
            testConfig.db.url,
            testConfig.db.username,
            testConfig.db.password
        )

        DSL.using(connection).use { ctx ->
            val tokens = Tokens.TOKENS

            ctx.insertInto(tokens, tokens.WALLET, tokens.TOKEN)
                .values(wallet, token)
                .execute()
        }
    }

    /**
     * Test US-001 Deposit of ETH
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and two ethereum wallets and "fromAddress" with at least 0.001 Ether
     * (1234000000000000 Wei) and notary running
     * @when "fromAddress" transfers 1234000000000000 Wei to "toAddress"
     * @then Associated Iroha account balance is increased on 1234000000000000 Wei
     */
    // TODO: I'm broken, fix me please
    @Disabled
    @Test
    fun depositOfETH() {
        val assetId = "ether#ethereum"
        val amount = BigInteger.valueOf(1_234_000_000_000_000)

        // ensure that initial wallet value is 0
        assertEquals(BigInteger.ZERO, queryIroha(assetId))

        setAccountDetail("notary_red@notary", toAddress, "user1@notary")

        // run notary
        async {
            main(arrayOf())
        }
        Thread.sleep(3_000)

        // send ETH
        deployHelper.sendEthereum(amount, toAddress)
        Thread.sleep(5_000)

        // Send again any transaction to commit in Ethereum network
        deployHelper.sendEthereum(amount, toAddress)
        Thread.sleep(20_000)

        assertEquals(amount, queryIroha(assetId))
    }

    /**
     * Test US-002 Deposit of ETH
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and two ethereum wallets and "fromAddress" with at least 51 coin
     * (51 coin) and notary running
     * @when "fromAddress" transfers 51 coin to "toAddress"
     * @then Associated Iroha account balance is increased on 51 coin
     */
    @Disabled
    @Test
    fun depositOfERC20() {
        val asset = "coin"
        val assetId = "$asset#ethereum"
        val amount = BigInteger.valueOf(51)

        // ensure that initial wallet value is 0
        assertEquals(BigInteger.ZERO, queryIroha(assetId))

        // Deploy ERC20 smart contract
        val contract = DeployHelper(testConfig.ethereum).deployBasicCoinSmartContract()
        val contractAddress = contract.contractAddress
        insertToken(contractAddress, asset)

        // run notary
        async {
            main(arrayOf())
        }
        Thread.sleep(3_000)


        // send ETH
        contract.transfer(toAddress, amount).send()
        assertEquals(amount, contract.balanceOf(toAddress).send())
        Thread.sleep(5_000)


        // Send again any transaction to commit in Ethereum network
        contract.transfer(toAddress, amount).send()
        Thread.sleep(20_000)

        assertEquals(amount, queryIroha(assetId))
    }

    /**
     * Test US-003 Withdrawal of ETH token
     * Note: Iroha must be deployed to pass the test.
     * @given Iroha networks running and user has sent 64203 Wei to master
     * @when withdrawal service queries notary
     * @then notary replies with refund information and signature
     */
    @Disabled
    @Test
    fun testRefund() {
        async {
            main(arrayOf())
        }

        val masterAccount = testConfig.notaryIrohaAccount
        val amount = "64203"
        val assetId = "ether#ethereum"
        val ethWallet = "eth_wallet"

        // add assets to user
        addAssetIroha(assetId, amount)

        // transfer assets from user to notary master account
        val hash = transferAssetIroha(creator, masterAccount, assetId, amount, ethWallet)

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

        assertEquals(
            signUserData(
                testConfig.ethereum,
                hashToWithdraw(
                    assetId.split("#")[0],
                    amount.toBigInteger(),
                    ethWallet,
                    hash
                )
            ), response.ethSignature
        )
    }

    /**
     * Full withdrawal pipeline test
     * @given iroha and withdrawal services are running, free relays available, user account has 125 Wei in Iroha
     * @when user transfers 125 Wei to Iroha master account
     * @then balance of user's wallet in Ethereum increased by 125 Wei
     */
    @Disabled
    @Test
    fun testFullWithdrawalPipeline() {
        val registerServicePort = 8083
        val name = getRandomString()
        val fullName = "$name@notary"
        val kp = ModelCrypto().generateKeypair()

        async {
            withdrawalservice.main(emptyArray())
        }
        Thread.sleep(5000)

        val res = khttp.post(
            "http://127.0.0.1:$registerServicePort/users",
            data = mapOf("name" to name, "pubkey" to kp.publicKey().hex())
        )
        assertEquals(200, res.statusCode)

        val initialBalance =
            deployHelper.web3.ethGetBalance(toAddress, DefaultBlockParameterName.LATEST).send().balance

        val amount = "125"
        val assetId = "ether#ethereum"
        val ethWallet = toAddress

        // add assets to user
        addAssetIroha(assetId, amount)
        Thread.sleep(5000)
        transferAssetIroha(creator, fullName, assetId, amount, "")
        Thread.sleep(5000)

        // transfer assets from user to notary master account
        transferAssetIroha(fullName, masterAccount, assetId, amount, ethWallet, fullName, kp)
        Thread.sleep(15000)


        assertEquals(
            initialBalance + BigInteger.valueOf(amount.toLong()),
            deployHelper.web3.ethGetBalance(toAddress, DefaultBlockParameterName.LATEST).send().balance
        )
    }
}
