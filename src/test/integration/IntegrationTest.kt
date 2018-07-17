package integration

import com.github.kittinunf.result.failure
import com.google.protobuf.InvalidProtocolBufferException
import com.squareup.moshi.Moshi
import config.loadConfigs
import io.grpc.ManagedChannelBuilder
import iroha.protocol.Queries.Query
import iroha.protocol.QueryServiceGrpc
import jp.co.soramitsu.iroha.ModelProtoQuery
import jp.co.soramitsu.iroha.ModelQueryBuilder
import jp.co.soramitsu.iroha.ModelTransactionBuilder
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
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.utils.Numeric
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.toBigInteger
import sidechain.iroha.util.toByteArray
import java.math.BigInteger
import java.sql.DriverManager

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

    private val deploy_helper = DeployHelper()

    /** Iroha host */
    val irohaHost = testConfig.iroha.hostname

    /** Iroha port */
    val irohaPort = testConfig.iroha.port

    /** Iroha transaction creator */
    val creator = testConfig.iroha.creator

    /** Iroha keypair */
    val keypair = ModelUtil.loadKeypair(testConfig.iroha.pubkeyPath, testConfig.iroha.privkeyPath).get()

    val irohaNetwork = IrohaNetworkImpl(irohaHost, irohaPort)

    /** Ethereum address to transfer from */
    private val fromAddress = "0x004ec07d2329997267Ec62b4166639513386F32E"

    /** Ethereum address to transfer to */
    private val toAddress = "0x00aa39d30f0d20ff03a22ccfc30b7efbfca597c2"

    /**
     * Send Ethereum transaction to wallet with specified data.
     */
    private fun sendEthereum(amount: BigInteger) {
        // get the next available nonce
        val ethGetTransactionCount = deploy_helper.web3.ethGetTransactionCount(
            fromAddress, DefaultBlockParameterName.LATEST
        ).send()
        val nonce = ethGetTransactionCount.transactionCount

        // create our transaction
        val rawTransaction = RawTransaction.createTransaction(
            nonce,
            deploy_helper.gasPrice,
            deploy_helper.gasLimit,
            toAddress,
            amount,
            ""
        )

        // sign & send our transaction
        val signedMessage = TransactionEncoder.signMessage(rawTransaction, deploy_helper.credentials)
        val hexValue = Numeric.toHexString(signedMessage)
        deploy_helper.web3.ethSendRawTransaction(hexValue).send()
    }

    /**
     * Sends transaction to Iroha.
     * @return hex representation of transaction
     */
    fun sendTxToIroha(txBuilder: ModelTransactionBuilder): String {
        val utx = txBuilder.build()
        val hash = utx.hash()

        val tx = ModelUtil.prepareTransaction(utx, keypair)
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
    fun queryIroha(assetId: String): BigInteger {
        val accountId = "user1@notary"
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
            println("account " + asset.accountId)
            println("asset ${asset.assetId} - ${asset.balance.value.toBigInteger()}")
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
        sendEthereum(amount)
        Thread.sleep(5_000)

        // Send again any transaction to commit in Ethereum network
        sendEthereum(amount)
        Thread.sleep(25_000)

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
        val contract = DeployHelper().deployBasicCoinSmartContract()
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
     * @then notary replyes with refund information and signature
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

        assertEquals("mockSignature", response.ethSignature)
    }
}
