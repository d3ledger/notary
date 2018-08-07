package integration

import com.github.kittinunf.result.failure
import com.google.protobuf.InvalidProtocolBufferException
import config.EthereumPasswords
import config.TestConfig
import config.loadConfigs
import io.grpc.ManagedChannelBuilder
import iroha.protocol.Queries
import iroha.protocol.QueryServiceGrpc
import jp.co.soramitsu.iroha.ModelProtoQuery
import jp.co.soramitsu.iroha.ModelQueryBuilder
import kotlinx.coroutines.experimental.async
import notary.db.tables.Tokens
import notary.main
import org.jooq.impl.DSL
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import sidechain.eth.util.DeployHelper
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.toByteArray
import java.math.BigInteger
import java.sql.DriverManager

/**
 * Integration tests for deposit case.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DepositIntegrationTest {

    init {
        IrohaInitialization.loadIrohaLibrary()
            .failure {
                println(it)
                System.exit(1)
            }

        // run notary
        async {
            main(arrayOf())
        }
        Thread.sleep(3_000)
    }

    /** Configurations for tests */
    val testConfig = loadConfigs("test", TestConfig::class.java)

    /** Ethereum password configs */
    val passwordConfig = loadConfigs("test", EthereumPasswords::class.java, "/ethereum_password.properties")

    /** Iroha transaction creator */
    val creator = testConfig.iroha.creator

    /** Iroha keypair */
    val keypair = ModelUtil.loadKeypair(testConfig.iroha.pubkeyPath, testConfig.iroha.privkeyPath).get()

    /** Iroha client account */
    val clientIrohaAccount = "user1@notary"

    /** Ethereum address to transfer to */
    private val relayWallet = testConfig.ropstenTestAccount

    /** Ethereum utils */
    private val deployHelper = DeployHelper(testConfig.ethereum, passwordConfig)

    /** Iroha network layer */
    val irohaConsumer = IrohaConsumerImpl(testConfig.iroha)

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
     * Query Iroha account balance
     */
    fun getAccountBalance(accountId: String, assetId: String): BigInteger {
        val queryCounter: Long = 1

        val uquery = ModelQueryBuilder()
            .creatorAccountId(creator)
            .queryCounter(BigInteger.valueOf(queryCounter))
            .createdTime(BigInteger.valueOf(System.currentTimeMillis()))
            .getAccountAssets(accountId)
            .build()
        val queryBlob = ModelProtoQuery(uquery).signAndAddSignature(keypair).finish().blob().toByteArray()

        val protoQuery: Queries.Query?
        try {
            protoQuery = Queries.Query.parseFrom(queryBlob)
        } catch (e: InvalidProtocolBufferException) {
            fail { "Exception while converting byte array to protobuf:" + e.message }
        }

        val channel =
            ManagedChannelBuilder.forAddress(testConfig.iroha.hostname, testConfig.iroha.port).usePlaintext(true)
                .build()
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
     * Set relay wallet for account
     * @param accountId - account in Iroha
     * @param relayWallet - relay wallet in Ethereum
     */
    private fun setRelayToAccount(accountId: String, relayWallet: String) {
        ModelUtil.setAccountDetail(
            irohaConsumer,
            testConfig.relayRegistrationIrohaAccount,
            "notary_red@notary",
            relayWallet,
            accountId
        )
    }

    /**
     * Test US-001 Deposit of ETH
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and two ethereum wallets and "fromAddress" with at least
     * 1234000000000 Wei and notary running and user registered with ethereum relayWallet
     * @when "fromAddress" transfers 1234000000000 Wei to "relayWallet"
     * @then Associated Iroha account balance is increased on 1234000000000 Wei
     */
    @Test
    fun depositOfETH() {
        val assetId = "ether#ethereum"
        val initialAmount = getAccountBalance(clientIrohaAccount, assetId)
        val amount = BigInteger.valueOf(1_234_000_000_000)

        setRelayToAccount(clientIrohaAccount, relayWallet)

        // send ETH
        deployHelper.sendEthereum(amount, relayWallet)
        Thread.sleep(120_000)

        Assertions.assertEquals(initialAmount + amount, getAccountBalance(clientIrohaAccount, assetId))
    }

    /**
     * Test US-001 Deposit of ETH
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and two ethereum wallets and "fromAddress" with at least
     * 1234000000000 Wei and notary running
     * @when "fromAddress" transfers 0 Wei to "relayWallet" and then "fromAddress" transfers 1234000000000 Wei
     * to "relayWallet"
     * @then Associated Iroha account balance is increased on 1234000000000 Wei
     */
    @Test
    fun depositZeroETH() {
        val assetId = "ether#ethereum"
        val initialAmount = getAccountBalance(clientIrohaAccount, assetId)
        val zeroAmount = BigInteger.ZERO
        val amount = BigInteger.valueOf(1_234_000_000_000)

        setRelayToAccount(clientIrohaAccount, relayWallet)

        // send 0 ETH
        deployHelper.sendEthereum(zeroAmount, relayWallet)
        Thread.sleep(120_000)

        Assertions.assertEquals(initialAmount, getAccountBalance(clientIrohaAccount, assetId))

        // Send again 1234000000000 Ethereum network
        deployHelper.sendEthereum(amount, relayWallet)
        Thread.sleep(120_000)

        Assertions.assertEquals(initialAmount + amount, getAccountBalance(clientIrohaAccount, assetId))
    }

    /**
     * Test US-002 Deposit of ETH
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and two ethereum wallets and "fromAddress" with at least 51 coin
     * (51 coin) and notary running
     * @when "fromAddress" transfers 51 coin to "relayWallet"
     * @then Associated Iroha account balance is increased on 51 coin
     */
    @Test
    fun depositOfERC20() {
        val asset = "coin"
        val assetId = "$asset#ethereum"
        val initialAmount = getAccountBalance(clientIrohaAccount, assetId)
        val amount = BigInteger.valueOf(51)

        // Deploy ERC20 smart contract
        val contract = DeployHelper(testConfig.ethereum, passwordConfig).deployERC20TokenSmartContract()
        val contractAddress = contract.contractAddress
        insertToken(contractAddress, asset)

        setRelayToAccount(clientIrohaAccount, relayWallet)

        // send ETH
        contract.transfer(relayWallet, amount).send()
        Thread.sleep(120_000)
        Assertions.assertEquals(initialAmount + amount, getAccountBalance(clientIrohaAccount, assetId))
    }

    /**
     * Test US-002 Deposit of ETH
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and two ethereum wallets and "fromAddress" with at least 51 coin
     * (51 coin) and notary running
     * @when "fromAddress" transfers 0 tokens to "relayWallet" and then "fromAddress" transfers 51 coin to "relayWallet"
     * @then Associated Iroha account balance is increased on 51 coin
     */
    @Test
    fun depositZeroOfERC20() {
        val asset = "coin"
        val assetId = "$asset#ethereum"
        val initialAmount = getAccountBalance(clientIrohaAccount, assetId)
        val zeroAmount = BigInteger.ZERO
        val amount = BigInteger.valueOf(51)

        // Deploy ERC20 smart contract
        val contract = DeployHelper(testConfig.ethereum, passwordConfig).deployERC20TokenSmartContract()
        val contractAddress = contract.contractAddress
        insertToken(contractAddress, asset)

        setRelayToAccount(clientIrohaAccount, relayWallet)

        // send 0 ERC20
        contract.transfer(relayWallet, zeroAmount).send()
        Thread.sleep(120_000)

        Assertions.assertEquals(initialAmount, getAccountBalance(clientIrohaAccount, assetId))

        // Send again
        contract.transfer(relayWallet, amount).send()
        Thread.sleep(120_000)
        Assertions.assertEquals(initialAmount + amount, getAccountBalance(clientIrohaAccount, assetId))
    }

}
