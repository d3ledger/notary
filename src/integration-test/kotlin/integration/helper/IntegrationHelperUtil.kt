package integration.helper

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.success
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import config.*
import contract.Master
import io.grpc.ManagedChannelBuilder
import iroha.protocol.QueryServiceGrpc
import jp.co.soramitsu.iroha.*
import mu.KLogging
import notary.NotaryConfig
import org.junit.jupiter.api.fail
import org.web3j.protocol.core.DefaultBlockParameterName
import provider.EthFreeRelayProvider
import provider.EthRelayProviderIrohaImpl
import provider.EthTokensProviderImpl
import registration.RegistrationConfig
import registration.RegistrationStrategyImpl
import registration.relay.RelayRegistration
import registration.relay.RelayRegistrationConfig
import sidechain.eth.util.DeployHelper
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import util.getRandomString
import withdrawalservice.WithdrawalServiceConfig
import java.math.BigInteger

/**
 * Utility class that makes testing more comfortable.
 * Class lazily creates new master contract in Etherem and master account in Iroha.
 */
class IntegrationHelperUtil {

    init {
        IrohaInitialization.loadIrohaLibrary()
            .failure { ex ->
                ex.printStackTrace()
                System.exit(1)
            }
    }

    /** Configurations for tests */
    val testConfig = loadConfigs("test", TestConfig::class.java, "/test.properties")

    /** Ethereum password configs */
    private val passwordConfig = loadConfigs("test", EthereumPasswords::class.java, "/ethereum_password.properties")

    /** Ethereum utils */
    private val deployHelper = DeployHelper(testConfig.ethereum, passwordConfig)

    /** Iroha keypair */
    val irohaKeyPair = ModelUtil.loadKeypair(testConfig.iroha.pubkeyPath, testConfig.iroha.privkeyPath).get()

    private val irohaConsumer = IrohaConsumerImpl(testConfig.iroha)

    /** Notary ethereum address that is used in master smart contract to verify proof provided by notary */
    private val notaryEthAddress = "0x6826d84158e516f631bbf14586a9be7e255b2d23"

    /** Account that used to store registered clients.*/
    val registrationAccount = createTesterAccount()

    /** Account that used to store tokens*/
    val tokenStorageAccount = createTesterAccount()

    /** New master ETH master contract*/
    val masterContract by lazy {
        val wallet = deployMasterEth()
        logger.info("master eth wallet $wallet was deployed ")
        wallet
    }

    /** Provider that is used to store/fetch tokens*/
    private val ethTokensProvider = EthTokensProviderImpl(
        testConfig.iroha,
        irohaKeyPair,
        testConfig.notaryIrohaAccount,
        tokenStorageAccount
    )

    private val relayRegistrationConfig =
        loadConfigs("test", RelayRegistrationConfig::class.java, "/test.properties")

    /** Provider that is used to get free registered relays*/
    private val ethFreeRelayProvider = EthFreeRelayProvider(
        testConfig.iroha,
        irohaKeyPair,
        testConfig.notaryIrohaAccount,
        registrationAccount
    )

    /** Provider of ETH wallets created by registrationAccount*/
    private val ethRelayProvider = EthRelayProviderIrohaImpl(
        testConfig.iroha, irohaKeyPair, testConfig.notaryIrohaAccount, registrationAccount
    )

    /** Provider of ETH wallets created by registrationAccount*/
    private val registrationStrategy =
        RegistrationStrategyImpl(
            ethFreeRelayProvider,
            irohaConsumer,
            testConfig.notaryIrohaAccount,
            registrationAccount
        )

    private val relayRegistration = RelayRegistration(relayRegistrationConfig, passwordConfig)

    /** Test configuration for Iroha */
    private fun createIrohaConfig(): IrohaConfig {
        return object : IrohaConfig {
            override val hostname: String
                get() = withdrawalConfig.iroha.hostname
            override val port: Int
                get() = withdrawalConfig.iroha.port
            override val creator: String
                get() = registrationAccount
            override val pubkeyPath: String
                get() = withdrawalConfig.iroha.pubkeyPath
            override val privkeyPath: String
                get() = withdrawalConfig.iroha.privkeyPath
        }
    }

    /** Configuration for notary instance */
    val notaryConfig = loadConfigs("notary", NotaryConfig::class.java, "/notary.properties")

    /** Test configuration of Notary with runtime dependencies */
    private fun createNotaryConfig(): NotaryConfig {
        return mock {
            on { registrationServiceIrohaAccount } doReturn registrationAccount
            on { tokenStorageAccount } doReturn tokenStorageAccount
            on { whitelistSetter } doReturn testConfig.whitelistSetter
            on { refund } doReturn notaryConfig.refund
            on { iroha } doReturn notaryConfig.iroha
            on { ethereum } doReturn notaryConfig.ethereum
            on { bitcoin } doReturn notaryConfig.bitcoin
        }
    }

    /**
     * Run Notary service instance with integration test environment
     */
    fun runNotary() {
        notary.executeNotary(createNotaryConfig())
    }

    /** Configuration for withdrawal service instance */
    private val withdrawalConfig =
        loadConfigs("withdrawal", WithdrawalServiceConfig::class.java, "/withdrawal.properties")

    /** Test configuration of Withdrawal service with runtime dependencies */
    private fun createWithdrawalConfig(): WithdrawalServiceConfig {
        val outerTokenStorageAccount = tokenStorageAccount
        return object : WithdrawalServiceConfig {
            override val notaryIrohaAccount: String
                get() = withdrawalConfig.notaryIrohaAccount
            override val tokenStorageAccount: String
                get() = outerTokenStorageAccount
            override val registrationIrohaAccount: String
                get() = registrationAccount
            override val iroha: IrohaConfig
                get() = createIrohaConfig()
            override val ethereum: EthereumConfig
                get() = withdrawalConfig.ethereum
        }
    }

    /**
     * Run Withdrawal service instance with integration test environment
     */
    fun runWithdrawal() {
        withdrawalservice.executeWithdrawal(createWithdrawalConfig(), passwordConfig)
    }

    /** Configuration for registration instance */
    val registrationConfig =
        loadConfigs("registration", RegistrationConfig::class.java, "/registration.properties")

    /** Test configuration of Registration with runtime dependencies */
    private fun createRegistrationConfig(): RegistrationConfig {
        return mock {
            on { port } doReturn registrationConfig.port
            on { relayRegistrationIrohaAccount } doReturn registrationAccount
            on { notaryIrohaAccount } doReturn registrationConfig.notaryIrohaAccount
            on { iroha } doReturn createIrohaConfig()
        }
    }

    /**
     * Run Registration service instance with integration test environment
     */
    fun runRegistration() {
        registration.executeRegistration(createRegistrationConfig())
    }

    /**
     * Returns ETH balance for a given address
     */
    fun getEthBalance(address: String): BigInteger {
        return deployHelper.web3.ethGetBalance(address, DefaultBlockParameterName.LATEST).send().balance
    }

    /**
     * Deploys randomly named ERC20 token
     * @return pair (tokenName, tokenAddress)
     */
    fun deployRandomERC20Token(): Pair<String, String> {
        val tokenName = String.getRandomString(5)
        return Pair(tokenName, deployERC20Token(tokenName))
    }

    /**
     * Deploy ERC20 token and register it to the notary system:
     * - create asset in Iroha
     * - add to Token provider service
     * - add to master contract
     * @return token name in iroha and address of ERC20 smart contract
     */
    fun deployERC20Token(tokenName: String): String {
        val tokenAddress = deployHelper.deployERC20TokenSmartContract().contractAddress
        ModelUtil.createAsset(irohaConsumer, testConfig.iroha.creator, tokenName, "ethereum", 0)
        addERC20Token(tokenName, tokenAddress)
        masterContract.addToken(tokenAddress).send()
        return tokenAddress
    }

    /**
     * Add token to Iroha token provider
     * @param tokenName - user defined token name
     * @param tokenAddress - token ERC20 smart contract address
     */
    private fun addERC20Token(tokenName: String, tokenAddress: String) {
        ethTokensProvider.addToken(tokenAddress, tokenName)
            .success { logger.info { "token $tokenAddress was deployed" } }
    }

    /**
     * Transfer [amount] ERC20 deployed at [contractAddress] tokens to [toAddress]
     * @param contractAddress - address of ERC20 contract
     * @param amount - amount to transfer
     * @param toAddress - destination address
     */
    fun sendERC20Token(contractAddress: String, amount: BigInteger, toAddress: String) =
        deployHelper.sendERC20(contractAddress, toAddress, amount)

    /**
     * Get [whoAddress] balance of ERC20 tokens with [contractAddress]
     * @param contractAddress - address of ERC20 smart contract
     * @param whoAddress - address of client
     */
    fun getERC20TokenBalance(contractAddress: String, whoAddress: String): BigInteger =
        deployHelper.getERC20Balance(contractAddress, whoAddress)

    /**
     * Returns master contract ETH balance
     */
    fun getMasterEthBalance(): BigInteger {
        return getEthBalance(masterContract.contractAddress)
    }

    /**
     * Deploys ETH master contract
     */
    private fun deployMasterEth(): Master {
        ethTokensProvider.getTokens()
            .fold(
                { tokens ->
                    val master = deployHelper.deployMasterSmartContract()
                    for (token in tokens) {
                        master.addToken(token.key).send()
                        logger.info { "add token ${token.key} to master" }
                    }
                    master.addPeer(notaryEthAddress).send()
                    return master
                },
                { ex -> throw ex })
    }

    /**
     * Deploys relay contracts in Ethereum network
     */
    fun deployRelays(relaysToDeploy: Int) {
        relayRegistration.deploy(relaysToDeploy, masterContract.contractAddress, registrationAccount)
        logger.info("relays were deployed")
        Thread.sleep(10_000)
    }

    /**
     * Registers first free relay contract in Iroha with given name and public key
     */
    fun registerRelay(name: String): String {
        val keypair = ModelCrypto().generateKeypair()
        registrationStrategy.register(name, keypair.publicKey().hex())
            .fold({ registeredEthWallet ->
                logger.info("registered eth wallet $registeredEthWallet")
                return registeredEthWallet
            },
                { ex -> throw RuntimeException("$name was not registered", ex) })
    }

    /**
     * Registers first free relay contract in Iroha with random name and public key
     */
    fun registerRandomRelay(): String {

        val ethWallet = registerRelay(String.getRandomString(9))
        Thread.sleep(10_000)
        return ethWallet
    }

    /**
     * Send ETH with given amount to ethPublicKey
     */
    fun sendEth(amount: BigInteger, to: String) = deployHelper.sendEthereum(amount, to)

    /**
     * Send [amount] ERC20 token deployed on [tokenAddress] to [toAddress]
     * @param tokenAddress - ERC20 token smart contract
     * @param amount - amount of tokens to send
     * @param toAddress - address to send
     */
    fun sendToken(tokenAddress: String, amount: BigInteger, toAddress: String) =
        deployHelper.sendERC20(tokenAddress, toAddress, amount)

    /**
     * Returns wallets registered by master account in Iroha
     */
    fun getRegisteredEthWallets(): Set<String> = ethRelayProvider.getRelays().get().keys

    /**
     * Add asset to Iroha account
     * Add asset to creator and then transfer to destination account.
     * @param accountId - destination account
     * @param assetId - asset to add
     * @param amount - amount to add
     */
    fun addIrohaAssetTo(accountId: String, assetId: String, amount: String) {
        val creator = testConfig.iroha.creator

        ModelUtil.addAssetIroha(irohaConsumer, creator, assetId, amount)
        ModelUtil.transferAssetIroha(irohaConsumer, creator, creator, accountId, assetId, "", amount)
    }

    /**
     * Returns balance in Iroha
     * Query Iroha account balance
     * @param accountId - account in Iroha
     * @param assetId - asset in Iroha
     * @return balance of account asset
     */
    fun getIrohaAccountBalance(accountId: String, assetId: String): BigInteger {
        val queryCounter: Long = 1

        val uquery = ModelQueryBuilder()
            .creatorAccountId(testConfig.iroha.creator)
            .queryCounter(BigInteger.valueOf(queryCounter))
            .createdTime(BigInteger.valueOf(System.currentTimeMillis()))
            .getAccountAssets(accountId)
            .build()

        return ModelUtil.prepareQuery(uquery, irohaKeyPair)
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
                { ex ->
                    fail { "Exception while converting byte array to protobuf:  ${ex.message}" }
                }
            )
    }

    /**
     * Creates randomly named tester account in Iroha
     */
    private fun createTesterAccount(): String {
        val name = String.getRandomString(9)
        val domain = "notary"
        val creator = testConfig.iroha.creator
        irohaConsumer.sendAndCheck(
            ModelTransactionBuilder()
                .creatorAccountId(creator)
                .createdTime(ModelUtil.getCurrentTime())
                .createAccount(name, domain, irohaKeyPair.publicKey())
                .appendRole("$name@$domain", "tester")
                .build()
        )
        logger.info("account $name@notary was created")
        return "$name@notary"
    }

    /**
     * Register client
     */
    fun registerClient(): String {
        val name = String.getRandomString(9)
        val domain = "notary"
        val creator = testConfig.iroha.creator
        irohaConsumer.sendAndCheck(
            ModelTransactionBuilder()
                .creatorAccountId(creator)
                .createdTime(ModelUtil.getCurrentTime())
                .createAccount(name, domain, irohaKeyPair.publicKey())
                .build()
        )
        logger.info("client account $name@notary was created")
        return "$name@notary"
    }

    /**
     * Add Ethrereum addresses to client whitelist, so that she can withdraw only for that addresses
     * @param clientAccount - client account id in Iroha network
     * @param addresses - ethereum addresses where client can withdraw her assets
     */
    fun setWhitelist(clientAccount: String, addresses: List<String>) {
        val text = addresses.joinToString()

        ModelUtil.setAccountDetail(
            irohaConsumer,
            testConfig.whitelistSetter,
            clientAccount,
            "eth_whitelist",
            text
        )
    }

    /**
     * Transfer asset in iroha with custom creator
     * @param creator - iroha transaction creator
     * @param kp - keypair
     * @param srcAccountId - source account id
     * @param destAccountId - destination account id
     * @param assetId - asset id
     * @param description - transaction description
     * @param amount - amount
     * @return hex representation of transaction hash
     */
    fun transferAssetIrohaFromClient(
        creator: String,
        kp: Keypair,
        srcAccountId: String,
        destAccountId: String,
        assetId: String,
        description: String,
        amount: String
    ): String {
        val utx = ModelTransactionBuilder()
            .creatorAccountId(creator)
            .createdTime(ModelUtil.getCurrentTime())
            .transferAsset(srcAccountId, destAccountId, assetId, description, amount)
            .build()
        val hash = utx.hash()
        return ModelUtil.prepareTransaction(utx, kp)
            .flatMap { IrohaNetworkImpl(testConfig.iroha.hostname, testConfig.iroha.port).sendAndCheck(it, hash) }
            .get()
    }

    /**
     * Send HTTP POST request to registration service to register user
     * @param name - user name
     * @param pubkey - user public key
     */
    fun sendRegistrationRequest(name: String, pubkey: PublicKey): khttp.responses.Response {
        return khttp.post(
            "http://127.0.0.1:${registrationConfig.port}/users",
            data = mapOf("name" to name, "pubkey" to pubkey.hex())
        )

    }

    /**
     * Logger
     */
    companion object : KLogging()
}
