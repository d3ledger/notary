package integration.helper

import com.github.kittinunf.result.*
import config.loadConfigs
import contract.Master
import contract.RelayRegistry
import jp.co.soramitsu.iroha.Keypair
import jp.co.soramitsu.iroha.ModelCrypto
import jp.co.soramitsu.iroha.ModelTransactionBuilder
import jp.co.soramitsu.iroha.PublicKey
import integration.TestConfig
import io.grpc.ManagedChannelBuilder
import iroha.protocol.QueryServiceGrpc
import jp.co.soramitsu.iroha.*
import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging
import notary.eth.EthNotaryConfig
import notary.eth.executeNotary
import org.bitcoinj.core.Address
import org.bitcoinj.wallet.Wallet
import org.junit.jupiter.api.fail
import org.web3j.protocol.core.DefaultBlockParameterName
import provider.btc.BtcAddressesProvider
import provider.btc.BtcRegisteredAddressesProvider
import provider.eth.EthFreeRelayProvider
import provider.eth.EthRelayProviderIrohaImpl
import provider.eth.EthTokenInfo
import provider.eth.EthTokensProviderImpl
import registration.btc.BtcRegistrationStrategyImpl
import registration.eth.EthRegistrationStrategyImpl
import registration.eth.relay.RelayRegistration
import sidechain.eth.EthChainListener
import sidechain.eth.util.DeployHelper
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.getAccountAsset
import util.getRandomString
import java.io.File
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

    private val irohaConsumer by lazy {
        IrohaConsumerImpl(
            configHelper.testConfig.iroha.creator,
            configHelper.testConfig.iroha
        )
    }

    private val tokenProviderIrohaConsumer by lazy {
        IrohaConsumerImpl(
            accountHelper.tokenSetterAccount,
            configHelper.testConfig.iroha
        )
    }

    private val whiteListIrohaConsumer by lazy {
        IrohaConsumerImpl(
            configHelper.testConfig.whitelistSetter,
            configHelper.testConfig.iroha
        )
    }

    private val notaryListIrohaConsumer by lazy {
        IrohaConsumerImpl(
            accountHelper.notaryListSetterAccount,
            configHelper.testConfig.iroha
        )
    }

    private val mstRegistrationIrohaConsumer by lazy {
        IrohaConsumerImpl(
            accountHelper.mstRegistrationAccount,
            configHelper.testConfig.iroha
        )
    }

    /** Notary ethereum address that is used in master smart contract to verify proof provided by notary */
    private val notaryEthAddress = "0x6826d84158e516f631bbf14586a9be7e255b2d23"

    val relayRegistryContract by lazy {
        val contract = deployRelayRegistry()
        logger.info { "relay registry eth wallet ${contract.contractAddress} was deployed" }
        contract
    }


    /** New master ETH master contract*/
    val masterContract by lazy {
        val wallet = deployMasterEth(relayRegistryContract.contractAddress)
        logger.info("master eth wallet ${wallet.contractAddress} was deployed ")
        wallet
    }

    /** Provider that is used to store/fetch tokens*/
    val ethTokensProvider by lazy {
        EthTokensProviderImpl(
            configHelper.testConfig.iroha,
            irohaKeyPair,
            accountHelper.tokenStorageAccount,
            accountHelper.tokenSetterAccount
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

    /** Provider of ETH wallets created by registrationAccount*/
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
        val btcAddressesProvider =
            BtcAddressesProvider(
                testConfig.iroha,
                irohaKeyPair,
                accountHelper.mstRegistrationAccount,
                accountHelper.notaryAccount
            )
        val btcTakenAddressesProvider =
            BtcRegisteredAddressesProvider(
                testConfig.iroha,
                irohaKeyPair,
                accountHelper.registrationAccount,
                accountHelper.notaryAccount
            )
        BtcRegistrationStrategyImpl(
            btcAddressesProvider,
            btcTakenAddressesProvider,
            irohaConsumer,
            accountHelper.notaryAccount,
            accountHelper.registrationAccount
        )
    }

    private val relayRegistration by lazy {
        RelayRegistration(configHelper.createRelayRegistrationConfig(), configHelper.ethPasswordConfig)
    }

    /**
     * Pregenerates one BTC address that can be registered later
     * @return randomly generated BTC address
     */
    fun preGenBtcAddress(): Result<Address, Exception> {
        val walletFile = File(configHelper.btcRegistrationConfig.btcWalletPath)
        val wallet = Wallet.loadFromFile(walletFile)
        val address = wallet.freshReceiveAddress()
        wallet.saveToFile(walletFile)
        return ModelUtil.setAccountDetail(
            mstRegistrationIrohaConsumer,
            accountHelper.notaryAccount,
            address.toBase58(),
            "free"
        ).map { address }
    }

    /**
     * Registers BTC client
     * @param irohaAccountName - client account in Iroha
     * @return btc address related to client
     */
    fun registerBtcAddress(irohaAccountName: String): String {
        val keypair = ModelCrypto().generateKeypair()
        preGenBtcAddress().fold({
            btcRegistrationStrategy.register(irohaAccountName, keypair.publicKey().hex())
                .fold({ btcAddress ->
                    return btcAddress
                }, { ex -> throw ex })
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
    fun deployRandomERC20Token(precision: Short = 0): Pair<EthTokenInfo, String> {
        val name = String.getRandomString(5)
        return Pair(EthTokenInfo(name, precision), deployERC20Token(name, precision))
    }

    /**
     * Deploy ERC20 token and register it to the notary system:
     * - create asset in Iroha
     * - add to Token provider service
     * - add to master contract
     * @return token name in iroha and address of ERC20 smart contract
     */
    fun deployERC20Token(name: String, precision: Short): String {
        logger.info { "create $name ERC20 token" }
        val tokenAddress = deployHelper.deployERC20TokenSmartContract().contractAddress
        addERC20Token(tokenAddress, name, precision)
        masterContract.addToken(tokenAddress).send()
        return tokenAddress
    }

    /**
     * Deploys smart contract which always fails. Use only if you know why do need it.
     * @return contract address
     */
    fun deployFailer(): String {
        return deployHelper.deployFailerContract().contractAddress
    }

    /**
     * Add token to Iroha token provider
     * @param tokenName - user defined token name
     * @param tokenAddress - token ERC20 smart contract address
     */
    fun addERC20Token(tokenAddress: String, tokenName: String, precision: Short) {
        ModelUtil.createAsset(irohaConsumer, tokenName, "ethereum", precision)
        ModelUtil.setAccountDetail(
            tokenProviderIrohaConsumer,
            accountHelper.tokenStorageAccount,
            tokenAddress,
            tokenName
        ).success {
            logger.info { "token $tokenName was added to ${accountHelper.tokenStorageAccount} by ${tokenProviderIrohaConsumer.creator}" }
        }
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
     * Deploys relay registry contract
     */
    private fun deployRelayRegistry(): RelayRegistry {
        val relayRegistry = deployHelper.deployRelayRegistrySmartContract()
        return relayRegistry
    }

    /**
     * Deploys ETH master contract
     */
    private fun deployMasterEth(relayRegistry: String): Master {
        val master = deployHelper.deployMasterSmartContract(relayRegistry)
        master.addPeer(notaryEthAddress).send()
        return master
    }

    /**
     * Deploys relay contracts in Ethereum network
     */
    fun deployRelays(relaysToDeploy: Int) {
        relayRegistration.deploy(relaysToDeploy, masterContract.contractAddress)
            .fold(
                {
                    logger.info("Relays were deployed by ${accountHelper.registrationAccount}")
                },
                {
                    logger.error("Relays were not deployed.", it)
                }
            )
    }

    /**
     * Registers relay account in Iroha with given address
     * @param address Ethereum address to register
     */
    fun registerRelayByAddress(address: String) {
        relayRegistration.registerRelayIroha(address)
        Thread.sleep(10_000)
    }

    /**
     * Deploys relay and registers first free relay contract in Iroha to the client with given [name] and public key
     */
    fun registerClient(name: String, keypair: Keypair = ModelCrypto().generateKeypair()): String {
        deployRelays(1)
        return registerClientWithoutRelay(name, keypair)
    }

    /**
     * Registers first free relay contract in Iroha to the client with given [name] and public key
     */
    fun registerClientWithoutRelay(name: String, keypair: Keypair = ModelCrypto().generateKeypair()): String {
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
        return ethWallet
    }

    /**
     * Waits for exactly one iroha block
     */
    fun waitOneIrohaBlock() {
        val creator = testConfig.iroha.creator
        val keypair =
            ModelUtil.loadKeypair(
                testConfig.iroha.pubkeyPath,
                testConfig.iroha.privkeyPath
            ).get()


        val listener = IrohaChainListener(
            testConfig.iroha.hostname,
            testConfig.iroha.port,
            creator, keypair
        )

        runBlocking {
            listener.getBlock()
        }

    }

    /**
     * Waits for exactly one Ethereum block
     */
    fun waitOneEtherBlock() {
        val listener = EthChainListener(
            deployHelper.web3,
            BigInteger.valueOf(testConfig.ethereum.confirmationPeriod)
        )
        runBlocking { listener.getBlock() }
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
        ModelUtil.addAssetIroha(irohaConsumer, assetId, amount)
        if (irohaConsumer.creator != accountId)
            ModelUtil.transferAssetIroha(irohaConsumer, irohaConsumer.creator, accountId, assetId, "", amount)
    }

    /**
     * Returns balance in Iroha
     * Query Iroha account balance
     * @param accountId - account in Iroha
     * @param assetId - asset in Iroha
     * @return balance of account asset
     */
    fun getIrohaAccountBalance(accountId: String, assetId: String): String {
        return getAccountAsset(
            testConfig.iroha,
            irohaKeyPair,
            irohaNetwork,
            accountId,
            assetId
        ).get()
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
     * Create account for client
     */
    fun createClientAccount(): String {
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
            whiteListIrohaConsumer,
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
            notaryListIrohaConsumer,
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
            .flatMap { tx -> irohaNetwork.sendAndCheck(tx, hash) }
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

    /**
     * Run Ethereum notary process
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
