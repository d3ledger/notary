package integration

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.success
import config.EthereumPasswords
import config.TestConfig
import config.loadConfigs
import io.grpc.ManagedChannelBuilder
import iroha.protocol.QueryServiceGrpc
import jp.co.soramitsu.iroha.ModelQueryBuilder
import kotlinx.coroutines.experimental.async
import notary.main
import org.junit.jupiter.api.*
import provider.EthTokensProviderImpl
import sidechain.eth.util.DeployHelper
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil
import java.math.BigInteger

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
    private val testConfig = loadConfigs("test", TestConfig::class.java)

    /** Ethereum password configs */
    private val passwordConfig = loadConfigs("test", EthereumPasswords::class.java, "/ethereum_password.properties")

    /** Iroha transaction creator */
    private val creator = testConfig.iroha.creator

    /** Iroha keypair */
    private val keypair = ModelUtil.loadKeypair(testConfig.iroha.pubkeyPath, testConfig.iroha.privkeyPath).get()

    /** Iroha client account */
    private val clientIrohaAccount = "user1@notary"

    /** Ethereum address to transfer to */
    private val relayWallet = testConfig.ropstenTestAccount

    /** Ethereum utils */
    private val deployHelper = DeployHelper(testConfig.ethereum, passwordConfig)

    /** Iroha network layer */
    private val irohaConsumer = IrohaConsumerImpl(testConfig.iroha)

    private val ethTokensProvider =
        EthTokensProviderImpl(testConfig.iroha, keypair, testConfig.notaryIrohaAccount, testConfig.tokenStorageAccount)

    /**
     * Insert token into database
     * @param wallet - ethereum token wallet
     * @param token - token name
     */
    fun insertToken(wallet: String, token: String) {
        ethTokensProvider.addToken(wallet, token)
            .success { println("token was inserted. ${ethTokensProvider.getTokens()}") }
    }

    /**
     * Query Iroha account balance
     * @param accountId - account in Iroha
     * @param assetId - asset in Iroha
     * @return balance of account asset
     */
    fun getAccountBalance(accountId: String, assetId: String): BigInteger {
        val queryCounter: Long = 1

        val uquery = ModelQueryBuilder()
            .creatorAccountId(creator)
            .queryCounter(BigInteger.valueOf(queryCounter))
            .createdTime(BigInteger.valueOf(System.currentTimeMillis()))
            .getAccountAssets(accountId)
            .build()

        return ModelUtil.prepareQuery(uquery, keypair)
            .fold(
                { protoQuery ->
                    val channel =
                        ManagedChannelBuilder.forAddress(testConfig.iroha.hostname, testConfig.iroha.port)
                            .usePlaintext(true)
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

                    BigInteger.ZERO
                },
                {
                    fail { "Exception while converting byte array to protobuf:" + it.message }
                }
            )
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
     * Set relay wallet for client
     */
    @BeforeEach
    fun setup() {
        setRelayToAccount(clientIrohaAccount, relayWallet)
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
