package integration.helper

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.success
import config.TestConfig
import config.loadConfigs
import contract.Master
import io.grpc.ManagedChannelBuilder
import iroha.protocol.QueryServiceGrpc
import jp.co.soramitsu.iroha.*
import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging
import notary.eth.EthNotaryConfig
import notary.eth.executeNotary
import org.junit.jupiter.api.fail
import org.web3j.protocol.core.DefaultBlockParameterName
import provider.eth.EthFreeRelayProvider
import provider.eth.EthRelayProviderIrohaImpl
import provider.eth.EthTokenInfo
import provider.eth.EthTokensProviderImpl
import registration.btc.BtcRegistrationStrategyImpl
import registration.eth.EthRegistrationStrategyImpl
import registration.eth.relay.RelayRegistration
import sidechain.eth.util.DeployHelper
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import util.getRandomString
import java.math.BigInteger

/**
 * Utility class that makes testing more comfortable.
 * Class lazily creates new master contract in Etherem and master account in Iroha.
 */
class IntegrationHelperUtil {

    init {
        IrohaInitialization.loadIrohaLibrary()
            .failure { ex ->
                fail("cannot load iroha lib", ex)
            }
    }

    private val testConfig = loadConfigs("test", TestConfig::class.java, "/test.properties")

    /** Iroha keypair */
    val irohaKeyPair =
        ModelUtil.loadKeypair(testConfig.iroha.pubkeyPath, testConfig.iroha.privkeyPath).get()

    val accountHelper by lazy { AccountHelper(irohaKeyPair) }

    val configHelper by lazy { ConfigHelper(accountHelper) }

    /** Ethereum utils */
    private val deployHelper by lazy { DeployHelper(configHelper.testConfig.ethereum, configHelper.ethPasswordConfig) }

    private val irohaNetwork by lazy {
        IrohaNetworkImpl(configHelper.testConfig.iroha.hostname, configHelper.testConfig.iroha.port)
    }

    private val irohaConsumer by lazy { IrohaConsumerImpl(configHelper.testConfig.iroha) }

    /** Notary ethereum address that is used in master smart contract to verify proof provided by notary */
    private val notaryEthAddress = "0x6826d84158e516f631bbf14586a9be7e255b2d23"


    /** New master ETH master contract*/
    val masterContract by lazy {
        val wallet = deployMasterEth()
        logger.info("master eth wallet ${wallet.contractAddress} was deployed ")
        wallet
    }

    /** Provider that is used to store/fetch tokens*/
    private val ethTokensProvider by lazy {
        EthTokensProviderImpl(
            configHelper.testConfig.iroha,
            irohaKeyPair,
            accountHelper.notaryAccount,
            accountHelper.tokenStorageAccount
        )
    }

    /** Provider that is used to get free registered relays*/
    private val ethFreeRelayProvider by lazy {
        EthFreeRelayProvider(
            configHelper.testConfig.iroha,
            irohaKeyPair,
            accountHelper.notaryAccount,
            accountHelper.registrationAccount
        )
    }

    /** Provider of ETH wallets created by mstRegistrationAccount*/
    private val ethRelayProvider by lazy {
        EthRelayProviderIrohaImpl(
            configHelper.testConfig.iroha,
            irohaKeyPair,
            accountHelper.notaryAccount,
            accountHelper.registrationAccount
        )
    }

    private val ethRegistrationStrategy by lazy {
        EthRegistrationStrategyImpl(
            ethFreeRelayProvider,
            irohaConsumer,
            accountHelper.notaryAccount,
            accountHelper.registrationAccount
        )
    }

    private val btcRegistrationStrategy by lazy {
        BtcRegistrationStrategyImpl(
            irohaConsumer,
            accountHelper.notaryAccount,
            accountHelper.registrationAccount,
            configHelper.testConfig.bitcoin.walletPath
        )
    }

    private val relayRegistration by lazy {
        RelayRegistration(configHelper.createRelayRegistrationConfig(), configHelper.ethPasswordConfig)
    }

    fun registerBtcAddress(irohaAccountName: String): String {
        val keypair = ModelCrypto().generateKeypair()
        btcRegistrationStrategy.register(irohaAccountName, keypair.publicKey().hex())
            .fold({ btcAddress ->
                logger.info { "newly registered btc address $btcAddress" }
                return btcAddress
            }, { ex -> throw ex })
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
        logger.info { "create $tokenName ERC20 token" }
        val tokenAddress = deployHelper.deployERC20TokenSmartContract().contractAddress
        ModelUtil.createAsset(irohaConsumer, accountHelper.notaryAccount, tokenName, "ethereum", 0)
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
        ethTokensProvider.addToken(tokenAddress, EthTokenInfo(tokenName, 0))
            .success { logger.info { "token $tokenAddress was deployed" } }
    }

    /**
     * Transfer [amount] ERC20 deployed at [contractAddress] tokens to [toAddress]
     * @param contractAddress - address of ERC20 contract
     * @param amount - amount to transfer
     * @param toAddress - destination address
     */
    fun sendERC20Token(contractAddress: String, amount: BigInteger, toAddress: String) {
        logger.info { "send ERC20 $contractAddress $amount to $toAddress" }
        deployHelper.sendERC20(contractAddress, toAddress, amount)
    }

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
        val master = deployHelper.deployMasterSmartContract()
        master.addPeer(notaryEthAddress).send()
        return master
    }

    /**
     * Deploys relay contracts in Ethereum network
     */
    fun deployRelays(relaysToDeploy: Int) {
        relayRegistration.deploy(relaysToDeploy, masterContract.contractAddress, accountHelper.registrationAccount)
        logger.info("relays were deployed by ${accountHelper.registrationAccount}")
        Thread.sleep(10_000)
    }

    /**
     * Registers first free relay contract in Iroha to the client with given [name] and public key
     */
    fun registerClient(name: String, keypair: Keypair = ModelCrypto().generateKeypair()): String {
        deployRelays(1)

        ethRegistrationStrategy.register(name, keypair.publicKey().hex())
            .fold({ registeredEthWallet ->
                logger.info("registered client $name with relay $registeredEthWallet")
                return registeredEthWallet
            },
                { ex -> throw RuntimeException("$name was not registered", ex) })
    }

    /**
     * Registers first free relay contract in Iroha with random name and public key
     */
    fun registerRandomRelay(): String {
        val ethWallet = registerClient(String.getRandomString(9))
        Thread.sleep(10_000)
        return ethWallet
    }

    /**
     * Waits for exactly one iroha block
     */
    fun waitIrohaBlock() {
        val creator = testConfig.iroha.creator
        val keypair by lazy {
            ModelUtil.loadKeypair(
                testConfig.iroha.pubkeyPath,
                testConfig.iroha.privkeyPath
            ).get()
        }

        val listner = IrohaChainListener(
            testConfig.iroha.hostname,
            testConfig.iroha.port,
            creator, keypair
        )
        runBlocking {
            listner.getBlock()
        }

    }
    /**
     * Send ETH with given amount to ethPublicKey
     */
    fun sendEth(amount: BigInteger, to: String) {
        logger.info { "send $amount Wei to $to " }
        deployHelper.sendEthereum(amount, to)
    }

    fun getAccountDetails(accountDetailHolder: String, accountDetailSetter: String): Map<String, String> {
        return sidechain.iroha.util.getAccountDetails(
            configHelper.testConfig.iroha,
            irohaKeyPair,
            irohaNetwork,
            accountDetailHolder,
            accountDetailSetter
        ).get()
    }

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
        val creator = accountHelper.notaryAccount

        ModelUtil.addAssetIroha(irohaConsumer, creator, assetId, amount)
        if (creator != accountId)
            ModelUtil.transferAssetIroha(irohaConsumer, creator, creator, accountId, assetId, "", amount)
    }

    /**
     * Returns balance in Iroha
     * Query Iroha account balance
     * @param accountId - account in Iroha
     * @param assetId - asset in Iroha
     * @return balance of account asset
     */
    fun getIrohaAccountBalance(accountId: String, assetId: String): String {
        val queryCounter: Long = 1

        val uquery = ModelQueryBuilder()
            .creatorAccountId(accountHelper.notaryAccount)
            .queryCounter(BigInteger.valueOf(queryCounter))
            .createdTime(BigInteger.valueOf(System.currentTimeMillis()))
            .getAccountAssets(accountId)
            .build()

        return ModelUtil.prepareQuery(uquery, irohaKeyPair)
            .fold(
                { protoQuery ->
                    val channel =
                        ManagedChannelBuilder.forAddress(
                            configHelper.testConfig.iroha.hostname,
                            configHelper.testConfig.iroha.port
                        )
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
                            return asset.balance
                    }

                    "0"
                },
                { ex ->
                    fail("Exception while converting byte array to protobuf", ex)
                }
            )
    }

    /**
     * Sends btc to a given address
     */
    fun sendBtc(address: String, amount: Int): Boolean {
        return runCommand("bitcoin-cli -regtest sendtoaddress $address $amount")
                && generateBtcBlocks(6)
    }

    /**
     * Creates 100 more blocks in bitcoin blockchain. May be used as transaction confirmation mechanism.
     */
    fun generateBtcBlocks(blocks: Int = 100): Boolean {
        return runCommand("bitcoin-cli -regtest generate $blocks")
    }

    /**
     * Runs command line
     */
    private fun runCommand(cmd: String): Boolean {
        logger.info { "command to run $cmd" }
        val pr = Runtime.getRuntime().exec(cmd)
        pr.waitFor()
        return pr.exitValue() == 0
    }


    /**
     * Register client
     */
    fun registerClient(): String {
        val name = "client_${String.getRandomString(9)}"
        val domain = "notary"
        val creator = accountHelper.registrationAccount
        irohaConsumer.sendAndCheck(
            ModelTransactionBuilder()
                .creatorAccountId(creator)
                .createdTime(ModelUtil.getCurrentTime())
                .createAccount(name, domain, irohaKeyPair.publicKey())
                .build()
        ).fold({
            logger.info("client account $name@$domain was created")
            return "$name@notary"
        }, { ex -> throw Exception("cannot create client", ex) })

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
            configHelper.testConfig.whitelistSetter,
            clientAccount,
            "eth_whitelist",
            text
        )
    }

    /**
     * Add notary to notary list provider
     */
    fun addNotary(name: String, address: String) {
        ModelUtil.setAccountDetail(
            irohaConsumer,
            accountHelper.notaryListSetterAccount,
            accountHelper.notaryListStorageAccount,
            name,
            address
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
            .flatMap {
                IrohaNetworkImpl(
                    configHelper.testConfig.iroha.hostname,
                    configHelper.testConfig.iroha.port
                ).sendAndCheck(it, hash)
            }
            .get()
    }

    /**
     * Send HTTP POST request to registration service to register user
     * @param name - user name
     * @param pubkey - user public key
     * @param port - port of registration service
     */
    fun sendRegistrationRequest(name: String, pubkey: PublicKey, port: Int): khttp.responses.Response {
        return khttp.post(
            "http://127.0.0.1:${port}/users",
            data = mapOf("name" to name, "pubkey" to pubkey.hex())
        )
    }

    /*
        Runs Ethereum notary process
     */
    fun runEthNotary(ethNotaryConfig: EthNotaryConfig = configHelper.createEthNotaryConfig()) {
        executeNotary(ethNotaryConfig)

        val name = String.getRandomString(9)
        val address = "http://localhost:${ethNotaryConfig.refund.port}"
        addNotary(name, address)

        logger.info { "Notary $name is started on $address" }
    }

    /**
     * Logger
     */
    companion object : KLogging()

}
