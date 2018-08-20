package integration.helper

import com.squareup.moshi.Moshi
import config.EthereumPasswords
import config.TestConfig
import config.loadConfigs
import contract.Master
import io.grpc.ManagedChannelBuilder
import iroha.protocol.Queries
import iroha.protocol.QueryServiceGrpc
import jp.co.soramitsu.iroha.ModelCrypto
import jp.co.soramitsu.iroha.ModelProtoQuery
import jp.co.soramitsu.iroha.ModelQueryBuilder
import jp.co.soramitsu.iroha.ModelTransactionBuilder
import mu.KLogging
import org.web3j.protocol.core.DefaultBlockParameterName
import provider.EthFreeRelayProvider
import provider.EthRelayProviderIrohaImpl
import provider.EthTokensProviderImpl
import registration.RegistrationStrategyImpl
import registration.relay.RelayRegistration
import registration.relay.RelayRegistrationConfig
import sidechain.eth.util.DeployHelper
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.toByteArray
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
    private val passwordConfig = loadConfigs("ganache", EthereumPasswords::class.java, "/ethereum_password.properties")

    /** Ethereum utils */
    private val deployHelper = DeployHelper(testConfig.ethereum, passwordConfig)

    /** Iroha keypair */
    val irohaKeyPair = ModelUtil.loadKeypair(testConfig.iroha.pubkeyPath, testConfig.iroha.privkeyPath).get()

    private val irohaConsumer = IrohaConsumerImpl(testConfig.iroha)

    /** New Iroha data setter account*/
    val dataSetterAccount= testConfig.relayRegistrationIrohaAccount

    /** New master ETH master contract*/
    val masterEthWallet by lazy {
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

    private val relayRegistrationConfig =
        loadConfigs("test", RelayRegistrationConfig::class.java, "/test.properties")

    private val ethFreeRelayProvider = EthFreeRelayProvider(
        testConfig.iroha,
        irohaKeyPair,
        testConfig.notaryIrohaAccount,
        dataSetterAccount
    )
    /** Iroha network */
    private val irohaNetwork = IrohaNetworkImpl(testConfig.iroha.hostname, testConfig.iroha.port)

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
    fun deployMasterEth(): Master {
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
    fun sendEth(amount: BigInteger, to: String) = deployHelper.sendEthereum(amount, to)

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
            .creatorAccountId(testConfig.notaryIrohaAccount)
            .queryCounter(BigInteger.valueOf(queryCounter))
            .createdTime(BigInteger.valueOf(System.currentTimeMillis()))
            .getAccountAssets(accountId)
            .build()
        val queryBlob = ModelProtoQuery(uquery).signAndAddSignature(irohaKeyPair).finish().blob().toByteArray()
        val protoQuery: Queries.Query?
        protoQuery = Queries.Query.parseFrom(queryBlob)
        val channel =
            ManagedChannelBuilder.forAddress(testConfig.iroha.hostname, testConfig.iroha.port).usePlaintext(true)
                .build()
        val queryStub = QueryServiceGrpc.newBlockingStub(channel)
        val queryResponse = queryStub.find(protoQuery)

        val fieldDescriptor = queryResponse.descriptorForType.findFieldByName("account_assets_response")
        if (!queryResponse.hasField(fieldDescriptor)) {
            throw IllegalStateException("Query response error ${queryResponse.errorResponse}")
        }
        val assets = queryResponse.accountAssetsResponse.accountAssetsList
        for (asset in assets) {
            if (assetId == asset.assetId)
                return BigInteger(asset.balance)
        }
        return BigInteger.ZERO
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
        logger.info("registration_service account $name@notary was created")
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
