package integration.helper

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
import provider.eth.EthFreeRelayProvider
import provider.eth.EthRelayProviderIrohaImpl
import provider.eth.EthTokensProviderImpl
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
    private val testConfig = loadConfigs("test", TestConfig::class.java)

    /** Ethereum password configs */
    private val passwordConfig = loadConfigs("test", EthereumPasswords::class.java, "/ethereum_password.properties")

    /** Ethereum utils */
    private val deployHelper = DeployHelper(testConfig.ethereum, passwordConfig)

    /** Iroha keypair */
    private val irohaKeyPair = ModelUtil.loadKeypair(testConfig.iroha.pubkeyPath, testConfig.iroha.privkeyPath).get()

    private val irohaConsumer = IrohaConsumerImpl(testConfig.iroha)
    /** New Iroha data setter account*/
    val dataSetterAccount: String by lazy {
        createRegistrationAccount()
    }
    /** New master ETH master contract*/
    private val masterEthWallet by lazy {
        val wallet = deployMasterEth().contractAddress
        logger.info("master eth wallet $wallet was deployed ")
        wallet
    }

    private val ethTokensProvider = EthTokensProviderImpl(
        testConfig.iroha,
        irohaKeyPair,
        testConfig.notaryIrohaAccount,
        testConfig.tokenStorageAccount
    )

    private val relayRegistrationConfig = loadConfigs("relay-registration", RelayRegistrationConfig::class.java)

    private val ethFreeRelayProvider = EthFreeRelayProvider(
        testConfig.iroha,
        irohaKeyPair,
        testConfig.notaryIrohaAccount,
        dataSetterAccount
    )

    /** Provider of ETH wallets created by dataSetterAccount*/
    private val ethRelayProvider = EthRelayProviderIrohaImpl(
        testConfig.iroha, irohaKeyPair, testConfig.notaryIrohaAccount, dataSetterAccount
    )
    private val registrationStrategy =
        RegistrationStrategyImpl(
            ethFreeRelayProvider,
            irohaConsumer,
            testConfig.notaryIrohaAccount,
            dataSetterAccount
        )

    private val relayRegistration = RelayRegistration(relayRegistrationConfig, passwordConfig)

    /**
     * Returns ETH balance for a given address
     */
    fun getEthBalance(address: String): BigInteger {
        return deployHelper.web3.ethGetBalance(address, DefaultBlockParameterName.LATEST).send().balance
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
    private fun deployMasterEth(): Master {
        ethTokensProvider.getTokens()
            .fold(
                { tokens ->
                    return deployHelper.deployMasterSmartContract(tokens.keys.toList())
                },
                { ex -> throw ex })
    }

    /**
     * Deploys relay contracts in Ethereum network
     */
    fun deployRelays(relaysToDeploy: Int) {
        relayRegistration.deploy(relaysToDeploy, masterEthWallet, dataSetterAccount)
        logger.info("relays were deployed")
        Thread.sleep(30_000)
    }

    /**
     * Registers first free relay contract in Iroha with given name and public key
     */
    fun registerRelay(name: String, pubKey: String) {
        registrationStrategy.register(name, pubKey)
            .fold({ registeredEthWallet -> logger.info("registered eth wallet $registeredEthWallet") },
                { ex -> throw RuntimeException("$name was not registered", ex) })
    }

    /**
     * Registers first free relay contract in Iroha with random name and public key
     */
    fun registerRandomRelay() {
        val keypair = ModelCrypto().generateKeypair()
        registerRelay(String.getRandomString(9), keypair.publicKey().hex())
        Thread.sleep(10_000)
    }

    /**
     * Send ETH with given amount to ethPublicKey
     */
    fun sendEth(amount: BigInteger, ethPublicKey: String) {
        deployHelper.sendEthereum(amount, ethPublicKey)
    }

    /**
     * Returns wallets registered by master account in Iroha
     */
    fun getRegisteredEthWallets(): Set<String> = ethRelayProvider.getRelays().get().keys

    /**
     * Returns balance in Iroha
     */
    fun getIrohaBalance(assetId: String, accountId: String): BigInteger {
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
                    fail { "Exception while converting byte array to protobuf: ${ex.message}" }
                }
            )
    }

    /**
     * Sends btc to a given address
     */
    fun sendBtc(address: String, amount: Int): Boolean {
        return runCommand("bitcoin-cli -regtest sendtoaddress $address $amount")
                && generateBtcBlocks()
    }

    /**
     * Creates 100 more blocks in bitcoin blockchain. May be used as transaction confirmation mechanism.
     */
    private fun generateBtcBlocks(): Boolean {
        return runCommand("bitcoin-cli -regtest generate 100")
    }

    /**
     * Runs command line
     */
    private fun runCommand(cmd: String): Boolean {
        val pr = Runtime.getRuntime().exec(cmd)
        pr.waitFor()
        return pr.exitValue() == 0
    }

    /**
     * Creates randomly named registration account in Iroha
     */
    private fun createRegistrationAccount(): String {
        val name = String.getRandomString(9)
        val domain = "notary"
        val creator = testConfig.iroha.creator
        irohaConsumer.sendAndCheck(
            ModelTransactionBuilder()
                .creatorAccountId(creator)
                .createdTime(ModelUtil.getCurrentTime())
                .createAccount(name, domain, irohaKeyPair.publicKey())
                .appendRole("$name@$domain", "registration_service")
                .build()
        )
        logger.info("master account $name@notary was created")
        return "$name@notary"
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
