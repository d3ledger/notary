package integration

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.google.protobuf.InvalidProtocolBufferException
import com.squareup.moshi.Moshi
import config.EthereumPasswords
import config.TestConfig
import config.loadConfigs
import io.grpc.ManagedChannelBuilder
import iroha.protocol.Queries.Query
import iroha.protocol.QueryServiceGrpc
import jp.co.soramitsu.iroha.Keypair
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
import sidechain.eth.util.DeployHelper
import sidechain.eth.util.hashToWithdraw
import sidechain.eth.util.signUserData
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
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

    /** Configurations for tests */
    val testConfig = loadConfigs("test", TestConfig::class.java)

    /** Ethereum password configs */
    val passwordConfig = loadConfigs("test", EthereumPasswords::class.java, "/ethereum_password.properties")

    /** Iroha network layer */
    val irohaConsumer = IrohaConsumerImpl(testConfig.iroha)

    /** Ethereum utils */
    private val deployHelper = DeployHelper(testConfig.ethereum, passwordConfig)

    /** Iroha host */
    val irohaHost = testConfig.iroha.hostname

    /** Iroha port */
    val irohaPort = testConfig.iroha.port

    /** Iroha transaction creator */
    val creator = testConfig.iroha.creator

    /** Iroha keypair */
    val keypair = ModelUtil.loadKeypair(testConfig.iroha.pubkeyPath, testConfig.iroha.privkeyPath).get()

    /** Iroha client account */
    val clientIrohaAccount = "user1@notary"

    /** Ethereum address to transfer to */
    private val toAddress = testConfig.ropstenTestAccount

    /**
     * Set account detail in Iroha
     * @return hex representation of transaction hash
     */
    fun setAccountDetail(
        accountId: String, key: String, value: String,
        creatorAccountId: String = creator,
        kp: Keypair = keypair
    ): Result<String, Exception> {
        val utx = ModelTransactionBuilder()
            .creatorAccountId(creatorAccountId)
            .createdTime(ModelUtil.getCurrentTime())
            .setAccountDetail(accountId, key, value)
            .build()
        val hash = utx.hash()
        return ModelUtil.prepareTransaction(utx, kp)
            .flatMap { IrohaNetworkImpl(irohaHost, irohaPort).sendAndCheck(it, hash) }
    }

    /**
     * Query Iroha account balance
     */
    fun queryIroha(assetId: String, accountId: String): BigInteger {
        val queryCounter: Long = 1

        val uquery = ModelQueryBuilder()
            .creatorAccountId(creator)
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
                return BigInteger(asset.balance)
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
     * @given Ethereum and Iroha networks running and two ethereum wallets and "fromAddress" with at least
     * 1234000000000 Wei and notary running and user registered with ethereum toAddress
     * @when "fromAddress" transfers 1234000000000 Wei to "toAddress"
     * @then Associated Iroha account balance is increased on 1234000000000 Wei
     */
    @Test
    fun depositOfETH() {
        val assetId = "ether#ethereum"
        val amount = BigInteger.valueOf(1_234_000_000_000)

        // ensure that initial wallet value is 0
        assertEquals(BigInteger.ZERO, queryIroha(assetId, clientIrohaAccount))

        setAccountDetail("notary_red@notary", toAddress, clientIrohaAccount, testConfig.registrationIrohaAccount)

        // run notary
        async {
            main(arrayOf())
        }
        Thread.sleep(3_000)

        // send ETH
        deployHelper.sendEthereum(amount, toAddress)
        Thread.sleep(120_000)

        assertEquals(amount, queryIroha(assetId, clientIrohaAccount))
    }

    /**
     * Test US-001 Deposit of ETH
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and two ethereum wallets and "fromAddress" with at least
     * 1234000000000 Wei and notary running
     * @when Notary is running, test registers user with "toAddress" and then "fromAddress" transfers 1234000000000 Wei
     * to "toAddress"
     * @then Associated Iroha account balance is increased on 1234000000000 Wei
     */
    @Test
    fun depositAfterAddOfETH() {
        val assetId = "ether#ethereum"
        val amount = BigInteger.valueOf(1_234_000_000_000)

        // ensure that initial wallet value is 0
        assertEquals(BigInteger.ZERO, queryIroha(assetId, clientIrohaAccount))

        // run notary
        async {
            main(arrayOf())
        }
        Thread.sleep(3_000)

        setAccountDetail("notary_red@notary", toAddress, clientIrohaAccount, testConfig.registrationIrohaAccount)

        // send ETH
        deployHelper.sendEthereum(amount, toAddress)
        Thread.sleep(120_000)

        assertEquals(amount, queryIroha(assetId, clientIrohaAccount))
    }

    /**
     * Test US-001 Deposit of ETH
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and two ethereum wallets and "fromAddress" with at least
     * 1234000000000 Wei and notary running
     * @when "fromAddress" transfers 0 Wei to "toAddress" and then "fromAddress" transfers 1234000000000 Wei
     * to "toAddress"
     * @then Associated Iroha account balance is increased on 1234000000000 Wei
     */
    @Test
    fun depositZeroETH() {
        val assetId = "ether#ethereum"
        val zeroAmount = BigInteger.ZERO
        val amount = BigInteger.valueOf(1_234_000_000_000)

        // ensure that initial wallet value is 0
        assertEquals(BigInteger.ZERO, queryIroha(assetId, clientIrohaAccount))

        setAccountDetail("notary_red@notary", toAddress, clientIrohaAccount, testConfig.registrationIrohaAccount)

        // run notary
        async {
            main(arrayOf())
        }
        Thread.sleep(3_000)

        // send 0 ETH
        deployHelper.sendEthereum(zeroAmount, toAddress)
        Thread.sleep(220_000)

        // Send again 1234000000000 Ethereum network
        deployHelper.sendEthereum(amount, toAddress)
        Thread.sleep(120_000)

        assertEquals(amount, queryIroha(assetId, clientIrohaAccount))
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
        assertEquals(BigInteger.ZERO, queryIroha(assetId, clientIrohaAccount))

        // Deploy ERC20 smart contract
        val contract = DeployHelper(testConfig.ethereum, passwordConfig).deployERC20TokenSmartContract()
        Thread.sleep(120_000)
        val contractAddress = contract.contractAddress
        insertToken(contractAddress, asset)

        // run notary
        async {
            main(arrayOf())
        }
        Thread.sleep(3_000)


        // send ETH
        contract.transfer(toAddress, amount).send()
        Thread.sleep(120_000)
        assertEquals(amount, contract.balanceOf(toAddress).send())


        // Send again any transaction to commit in Ethereum network
        contract.transfer(toAddress, amount).send()
        Thread.sleep(120_000)
        assertEquals(amount, queryIroha(assetId, clientIrohaAccount))
    }

    /**
     * Test US-002 Deposit of ETH
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and two ethereum wallets and "fromAddress" with at least 51 coin
     * (51 coin) and notary running
     * @when "fromAddress" transfers 0 tokens to "toAddress" and then "fromAddress" transfers 51 coin to "toAddress"
     * @then Associated Iroha account balance is increased on 51 coin
     */
    @Test
    fun depositZeroOfERC20() {
        val asset = "coin"
        val assetId = "$asset#ethereum"
        val zeroAmount = BigInteger.ZERO
        val amount = BigInteger.valueOf(51)

        // ensure that initial wallet value is 0
        assertEquals(BigInteger.ZERO, queryIroha(assetId, clientIrohaAccount))

        // Deploy ERC20 smart contract
        val contract = DeployHelper(testConfig.ethereum, passwordConfig).deployERC20TokenSmartContract()
        Thread.sleep(120_000)
        val contractAddress = contract.contractAddress
        insertToken(contractAddress, asset)

        // run notary
        async {
            main(arrayOf())
        }
        Thread.sleep(3_000)

        // send 0 ERC20
        contract.transfer(toAddress, zeroAmount).send()
        Thread.sleep(120_000)
        assertEquals(amount, contract.balanceOf(toAddress).send())

        // Send again
        contract.transfer(toAddress, amount).send()
        Thread.sleep(120_000)
        assertEquals(amount, queryIroha(assetId, clientIrohaAccount))
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
        ModelUtil.addAssetIroha(irohaConsumer, creator, assetId, amount)

        // transfer assets from user to notary master account
        val hash =
            ModelUtil.transferAssetIroha(irohaConsumer, creator, creator, masterAccount, assetId, amount, ethWallet)
                .get()

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

        assertEquals(amount, response.ethRefund.amount)
        assertEquals(ethWallet, response.ethRefund.address)
        assertEquals("ether", response.ethRefund.assetId)

        assertEquals(
            signUserData(
                testConfig.ethereum,
                passwordConfig,
                hashToWithdraw(
                    assetId.split("#")[0],
                    amount,
                    ethWallet,
                    hash
                )
            ), response.ethSignature
        )
    }

}
