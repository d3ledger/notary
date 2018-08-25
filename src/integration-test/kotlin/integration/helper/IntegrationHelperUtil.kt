package integration.helper

import com.github.kittinunf.result.success
import config.EthereumPasswords
import config.TestConfig
import config.loadConfigs
import contract.Master
import io.grpc.ManagedChannelBuilder
import iroha.protocol.QueryServiceGrpc
import jp.co.soramitsu.iroha.ModelCrypto
import jp.co.soramitsu.iroha.ModelQueryBuilder
import jp.co.soramitsu.iroha.ModelTransactionBuilder
import mu.KLogging
import org.junit.jupiter.api.fail
import org.web3j.protocol.core.DefaultBlockParameterName
import provider.EthFreeRelayProvider
import provider.EthRelayProviderIrohaImpl
import provider.EthTokensProviderImpl
import registration.RegistrationStrategyImpl
import registration.relay.RelayRegistration
import registration.relay.RelayRegistrationConfig
import sidechain.eth.util.DeployHelper
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil
import util.getRandomString
import java.math.BigInteger

/**
 * Utility class that makes testing more comfortable.
 * Class lazily creates new master contract in Etherem and master account in Iroha.
 */
class IntegrationHelperUtil {
    /** Configurations for tests */
    private val testConfig = loadConfigs("test", TestConfig::class.java, "/test.properties")

    /** Ethereum password configs */
    private val passwordConfig = loadConfigs("test", EthereumPasswords::class.java, "/ethereum_password.properties")

    /** Ethereum utils */
    private val deployHelper = DeployHelper(testConfig.ethereum, passwordConfig)

    /** Iroha keypair */
    val irohaKeyPair = ModelUtil.loadKeypair(testConfig.iroha.pubkeyPath, testConfig.iroha.privkeyPath).get()

    private val irohaConsumer = IrohaConsumerImpl(testConfig.iroha)

    /** Account that used to store registered clients.*/
    val registrationAccount = createTesterAccount()

    /** Account that used to store tokens*/
    val tokenStorageAccount = createTesterAccount()

    /** New master ETH master contract*/
    val masterEthWallet by lazy {
        val wallet = deployMasterEth().contractAddress
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

    /**
     * Returns ETH balance for a given address
     */
    fun getEthBalance(address: String): BigInteger {
        return deployHelper.web3.ethGetBalance(address, DefaultBlockParameterName.LATEST).send().balance
    }

    /**
     * Deploys randomly named ERC20 token
     */
    fun deployRandomToken(): String {
        return deployToken(String.getRandomString(5))
    }

    /**
     * Deploys ERC20 token
     */
    fun deployToken(tokenName: String): String {
        val tokenAddress = deployHelper.deployERC20TokenSmartContract().contractAddress
        addToken(tokenName, tokenAddress)
        return tokenAddress
    }

    private fun addToken(tokenName: String, tokenAddress: String) {
        ethTokensProvider.addToken(tokenAddress, tokenName)
            .success { logger.info { "token $tokenAddress was deployed" } }
    }

    /**
     * Returns master contract ETH balance
     */
    fun getMasterEthBalance(): BigInteger {
        return getEthBalance(masterEthWallet)
    }

    /**
     * Deploys ETH master contract
     */
    fun deployMasterEth(): Master {
        ethTokensProvider.getTokens()
            .fold(
                { tokens ->
                    val master = deployHelper.deployMasterSmartContract()
                    for (token in tokens) {
                        master.addToken(token.key).send()
                        logger.info { "add token ${token.key} to master" }
                    }
                    return master
                },
                { ex -> throw ex })
    }

    /**
     * Deploys relay contracts in Ethereum network
     */
    fun deployRelays(relaysToDeploy: Int) {
        relayRegistration.deploy(relaysToDeploy, masterEthWallet, registrationAccount)
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

    fun sendToken(tokenAddress: String, amount: BigInteger, to: String) =
        deployHelper.sendERC20(tokenAddress, to, amount)

    /**
     * Returns wallets registered by master account in Iroha
     */
    fun getRegisteredEthWallets(): Set<String> = ethRelayProvider.getRelays().get().keys

    /**
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
     * Logger
     */
    companion object : KLogging()
}
