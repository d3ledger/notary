package integration.helper

import config.EthereumPasswords
import config.TestConfig
import config.loadConfigs
import contract.BasicCoin
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
import withdrawalservice.NotaryPeerListProviderImpl
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

    /** New Iroha data setter account*/
    val dataSetterAccount = testConfig.relayRegistrationIrohaAccount

    /** Notary ethereum address that is used in master smart contract to verify proof provided by notary */
    val notaryEthAddress = "0x6826d84158e516f631bbf14586a9be7e255b2d23"

    /** New master ETH master contract*/
    val masterContract by lazy {
        val wallet = deployMasterEth()
        logger.info("master eth wallet $wallet was deployed ")
        wallet
    }

    /** List of deployed ERC20 tokens */
    val tokens = mutableMapOf<String, BasicCoin>()

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
        return getEthBalance(masterContract.contractAddress)
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
                    }
                    master.addPeer(notaryEthAddress).send()
                    return master
                },
                { ex -> throw ex })
    }

    /**
     * Deploy ERC20 token and register it to the notary system
     */
    fun deployERC20Token() {
        val tokenName = String.getRandomString(5).toLowerCase()
        val contract = deployHelper.deployERC20TokenSmartContract()
        val hash = masterContract.addToken(contract.contractAddress).send().transactionHash
        ethTokensProvider.addToken(contract.contractAddress, tokenName)
        logger.info { "ERC20 token $tokenName was deployed on ${contract.contractAddress}, tx hash: $hash" }

        tokens.put(tokenName, contract)
    }

    /**
     * Deploys relay contracts in Ethereum network
     */
    fun deployRelays(relaysToDeploy: Int) {
        relayRegistration.deploy(relaysToDeploy, masterContract.contractAddress, dataSetterAccount)
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
