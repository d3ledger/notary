package integration.helper

import com.github.jleskovar.btcrpc.BitcoinRpcClientFactory
import com.github.kittinunf.result.*
import config.EthereumPasswords
import config.loadConfigs
import integration.TestConfig
import jp.co.soramitsu.iroha.Keypair
import jp.co.soramitsu.iroha.ModelCrypto
import jp.co.soramitsu.iroha.ModelTransactionBuilder
import jp.co.soramitsu.iroha.PublicKey
import kotlinx.coroutines.experimental.runBlocking
import model.IrohaCredential
import mu.KLogging
import notary.eth.EthNotaryConfig
import notary.eth.executeNotary
import org.bitcoinj.core.Address
import org.bitcoinj.core.ECKey
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.wallet.Wallet
import org.web3j.crypto.WalletUtils
import provider.btc.address.AddressInfo
import provider.btc.address.BtcAddressesProvider
import provider.btc.address.BtcRegisteredAddressesProvider
import provider.eth.EthFreeRelayProvider
import provider.eth.EthRelayProviderIrohaImpl
import provider.eth.EthTokensProviderImpl
import registration.ETH_WHITE_LIST_KEY
import registration.btc.BtcRegistrationStrategyImpl
import provider.btc.account.IrohaBtcAccountCreator
import registration.eth.EthRegistrationConfig
import registration.eth.EthRegistrationStrategyImpl
import registration.eth.relay.RelayRegistration
import sidechain.eth.EthChainListener
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.getAccountAsset
import token.EthTokenInfo
import util.getRandomString
import vacuum.RelayVacuumConfig
import withdrawalservice.WithdrawalServiceConfig
import java.io.Closeable
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger

const val btcAsset = "btc#bitcoin"

/**
 * Utility class that makes testing more comfortable.
 * Class lazily creates new master contract in Ethereum and master account in Iroha.
 */
class IntegrationHelperUtil : Closeable {

    init {
        IrohaInitialization.loadIrohaLibrary()
            .failure { ex ->
                logger.error("Cannot load Iroha library", ex)
                System.exit(1)
            }
    }

    override fun close() {
        irohaNetwork.close()
        irohaListener.close()
    }

    private val testConfig = loadConfigs("test", TestConfig::class.java, "/test.properties")

    val testCredential = IrohaCredential(
        testConfig.testCredentialConfig.accountId,
        ModelUtil.loadKeypair(
            testConfig.testCredentialConfig.pubkeyPath,
            testConfig.testCredentialConfig.privkeyPath
        ).get()
    )

    val accountHelper by lazy { AccountHelper(irohaNetwork) }

    val configHelper by lazy {
        ConfigHelper(
            accountHelper,
            relayRegistryContract.contractAddress,
            masterContract.contractAddress
        )
    }

    val ethRegistrationConfig by lazy { configHelper.createEthRegistrationConfig(testConfig.ethereum) }

    /** Ethereum utils */
    private val contractTestHelper by lazy { ContractTestHelper() }

    val irohaNetwork by lazy {
        IrohaNetworkImpl(testConfig.iroha.hostname, testConfig.iroha.port)
    }

    private val irohaConsumer by lazy {
        IrohaConsumerImpl(testCredential, irohaNetwork)
    }

    private val irohaListener = IrohaChainListener(
        testConfig.iroha.hostname,
        testConfig.iroha.port,
        testCredential
    )

    val ethListener = EthChainListener(
        contractTestHelper.deployHelper.web3,
        BigInteger.valueOf(testConfig.ethereum.confirmationPeriod)
    )

    private val registrationConsumer by lazy {
        IrohaConsumerImpl(accountHelper.registrationAccount, irohaNetwork)
    }

    private val tokenProviderIrohaConsumer by lazy {
        IrohaConsumerImpl(accountHelper.tokenSetterAccount, irohaNetwork)
    }

    private val notaryListIrohaConsumer by lazy {
        IrohaConsumerImpl(accountHelper.notaryListSetterAccount, irohaNetwork)
    }

    private val mstRegistrationIrohaConsumer by lazy {
        IrohaConsumerImpl(accountHelper.mstRegistrationAccount, irohaNetwork)
    }

    val relayRegistryContract by lazy {
        val contract = contractTestHelper.relayRegistry
        logger.info { "relay registry eth wallet ${contract.contractAddress} was deployed" }
        contract
    }

    /** New master ETH master contract*/
    val masterContract by lazy {
        val wallet = contractTestHelper.master
        logger.info("master eth wallet ${wallet.contractAddress} was deployed ")
        wallet
    }

    private val rpcClient by lazy {
        BitcoinRpcClientFactory.createClient(
            user = "test",
            password = "test",
            host = configHelper.createBtcNotaryConfig().bitcoin.host,
            port = 8332,
            secure = false
        )
    }

    /** Provider that is used to store/fetch tokens*/
    val ethTokensProvider by lazy {
        EthTokensProviderImpl(
            testCredential,
            irohaNetwork,
            accountHelper.tokenStorageAccount.accountId,
            accountHelper.tokenSetterAccount.accountId
        )
    }

    /** Provider that is used to get free registered relays*/
    private val ethFreeRelayProvider by lazy {
        EthFreeRelayProvider(
            accountHelper.registrationAccount,
            irohaNetwork,
            accountHelper.notaryAccount.accountId,
            accountHelper.registrationAccount.accountId
        )
    }

    /** Provider of ETH wallets created by registrationAccount*/
    private val ethRelayProvider by lazy {
        EthRelayProviderIrohaImpl(
            irohaNetwork,
            accountHelper.registrationAccount,
            accountHelper.notaryAccount.accountId,
            accountHelper.registrationAccount.accountId
        )
    }

    private val ethRegistrationStrategy by lazy {
        EthRegistrationStrategyImpl(
            ethFreeRelayProvider,
            ethRegistrationConfig,
            configHelper.ethPasswordConfig,
            registrationConsumer,
            accountHelper.notaryAccount.accountId
        )
    }

    private val btcRegistrationStrategy by lazy {
        val btcAddressesProvider =
            BtcAddressesProvider(
                testCredential,
                irohaNetwork,
                accountHelper.mstRegistrationAccount.accountId,
                accountHelper.notaryAccount.accountId
            )
        val btcTakenAddressesProvider =
            BtcRegisteredAddressesProvider(
                testCredential,
                irohaNetwork,
                accountHelper.registrationAccount.accountId,
                accountHelper.notaryAccount.accountId
            )
        val irohaBtcAccountCreator = IrohaBtcAccountCreator(
            registrationConsumer,
            accountHelper.notaryAccount.accountId
        )
        BtcRegistrationStrategyImpl(
            btcAddressesProvider,
            btcTakenAddressesProvider,
            irohaBtcAccountCreator
        )
    }

    private val relayRegistration by lazy {
        RelayRegistration(
            configHelper.createRelayRegistrationConfig(),
            accountHelper.registrationAccount,
            irohaNetwork,
            configHelper.ethPasswordConfig
        )
    }

    /**
     * Pregenerates multiple BTC address that can be registered later
     * @param walletFilePath - path to wallet file
     * @param addressesToGenerate - number of addresses to generate
     * @return result of operation
     */
    fun preGenBtcAddresses(walletFilePath: String, addressesToGenerate: Int): Result<Unit, Exception> {
        return Result.of {
            for (i in 1..addressesToGenerate) {
                preGenBtcAddress(walletFilePath).failure { ex -> throw ex }
            }
        }
    }

    private fun createMstAddress(keys: List<ECKey>): Address {
        val script = ScriptBuilder.createP2SHOutputScript(1, keys)
        return script.getToAddress(RegTestParams.get())
    }

    /**
     * Pregenerates one BTC address that can be registered later
     * @param walletFilePath - path to wallet file
     * @return randomly generated BTC address
     */
    fun preGenBtcAddress(walletFilePath: String): Result<Address, Exception> {
        val walletFile = File(walletFilePath)
        val wallet = Wallet.loadFromFile(walletFile)
        val key = wallet.freshReceiveKey()
        val address = createMstAddress(listOf(key))
        wallet.addWatchedAddress(address)
        wallet.saveToFile(walletFile)
        return ModelUtil.setAccountDetail(
            mstRegistrationIrohaConsumer,
            accountHelper.notaryAccount.accountId,
            address.toBase58(),
            AddressInfo.createFreeAddressInfo(listOf(key.publicKeyAsHex)).toJson()
        ).map { address }
    }

    /**
     * Registers BTC client
     * @param walletFilePath - path to wallet file
     * @param irohaAccountName - client account in Iroha
     * @param keypair - key pair for new client in Iroha
     * @return btc address related to client
     */
    fun registerBtcAddress(
        walletFilePath: String,
        irohaAccountName: String,
        keypair: Keypair = ModelCrypto().generateKeypair()
    ): String {
        preGenBtcAddress(walletFilePath).fold({
            return registerBtcAddressNoPreGen(irohaAccountName, keypair)
        }, { ex -> throw ex })
    }

    /**
     * Registers BTC client with no pregeneration
     * @param irohaAccountName - client account in Iroha
     * @param keypair - key pair of new client in Iroha
     * @param whitelist - list available addresses to send money to
     * @return btc address related to client
     */
    fun registerBtcAddressNoPreGen(
        irohaAccountName: String,
        keypair: Keypair = ModelCrypto().generateKeypair(),
        whitelist: List<String> = emptyList()
    ): String {
        btcRegistrationStrategy.register(irohaAccountName, whitelist, keypair.publicKey().hex())
            .fold({ btcAddress ->
                logger.info { "BTC address $btcAddress was registered by $irohaAccountName" }
                return btcAddress
            }, { ex -> throw ex })
    }

    /**
     * Returns ETH balance for a given address
     */
    fun getEthBalance(address: String): BigInteger {
        return contractTestHelper.getETHBalance(address)
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
        val tokenAddress = contractTestHelper.deployHelper.deployERC20TokenSmartContract().contractAddress
        addERC20Token(tokenAddress, name, precision)
        masterContract.addToken(tokenAddress).send()
        return tokenAddress
    }

    /**
     * Deploys smart contract which always fails. Use only if you know why do you need it.
     * @return contract address
     */
    fun deployFailer(): String {
        return contractTestHelper.deployFailer()
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
            accountHelper.tokenStorageAccount.accountId,
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
        contractTestHelper.sendERC20Token(contractAddress, amount, toAddress)
    }

    /**
     * Get [whoAddress] balance of ERC20 tokens with [contractAddress]
     * @param contractAddress - address of ERC20 smart contract
     * @param whoAddress - address of client
     */
    fun getERC20TokenBalance(contractAddress: String, whoAddress: String): BigInteger {
        return contractTestHelper.getERC20TokenBalance(contractAddress, whoAddress)
    }

    /**
     * Returns master contract ETH balance
     */
    fun getMasterEthBalance(): BigInteger {
        return getEthBalance(masterContract.contractAddress)
    }

    /**
     * Disable adding new notary peers to smart contract.
     * Before smart contract can be used it should be locked in order to prevent adding malicious peers.
     */
    fun lockEthMasterSmartcontract() {
        logger.info { "Disable adding new peers on master contract ${masterContract.contractAddress}" }
        masterContract.disableAddingNewPeers().send()
    }

    // Converts Bitcoins to Satoshi
    fun btcToSat(btc: Int): Long {
        return btc * 100_000_000L
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
     * Import relays from given file
     */
    fun importRelays(filename: String) {
        relayRegistration.import(filename)
            .fold(
                {
                    logger.info("Relays were imported by ${accountHelper.registrationAccount}")
                },
                {
                    logger.error("Relays were not imported.", it)
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
    fun registerClient(
        name: String,
        whitelist: List<String>,
        keypair: Keypair = ModelCrypto().generateKeypair()
    ): String {
        deployRelays(1)
        return registerClientWithoutRelay(name, whitelist, keypair)
    }

    /**
     * Registers first free relay contract in Iroha to the client with given [name] and public key
     */
    fun registerClientWithoutRelay(
        name: String,
        whitelist: List<String>,
        keypair: Keypair = ModelCrypto().generateKeypair()
    ): String {
        ethRegistrationStrategy.register(name, whitelist, keypair.publicKey().hex())
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
        // TODO: D3-417 Web3j cannot pass an empty list of addresses to the smart contract.
        val ethWallet = registerClient(String.getRandomString(9), listOf())
        return ethWallet
    }

    /**
     * Waits for exactly one iroha block
     */
    fun waitOneIrohaBlock() {
        runBlocking {
            val block = irohaListener.getBlock()
            logger.info { "Wait for one block ${block.payload.height}" }
        }
    }

    /**
     * Waits for exactly one Ethereum block
     */
    fun waitOneEtherBlock() {
        runBlocking { ethListener.getBlock() }
    }

    /**
     * Send ETH with given amount to ethPublicKey
     */
    fun sendEth(amount: BigInteger, to: String) {
        logger.info { "send $amount Wei to $to " }
        contractTestHelper.sendEthereum(amount, to)
    }

    fun getAccountDetails(accountDetailHolder: String, accountDetailSetter: String): Map<String, String> {
        return sidechain.iroha.util.getAccountDetails(
            testCredential,
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
            testCredential,
            irohaNetwork,
            accountId,
            assetId
        ).get()
    }

    /**
     * Sends btc to a given address
     */

    fun sendBtc(address: String, amount: Int, confirmations: Int = 6) {
        rpcClient.sendToAddress(address = address, amount = BigDecimal(amount))
        generateBtcBlocks(confirmations)
    }

    /**
     * Creates blocks in bitcoin blockchain. May be used as transaction confirmation mechanism.
     */
    fun generateBtcBlocks(blocks: Int = 150) {
        if (blocks > 0) {
            rpcClient.generate(numberOfBlocks = blocks)
            logger.info { "New $blocks ${singularOrPluralBlocks(blocks)} generated in Bitcoin blockchain" }
        }
    }

    private fun singularOrPluralBlocks(blocks: Int): String {
        if (blocks == 1) {
            return "block was"
        }
        return "blocks were"
    }

    /**
     * Add Ethrereum addresses to client whitelist, so that she can withdraw only for that addresses
     * @param clientAccount - client account id in Iroha network
     * @param addresses - ethereum addresses where client can withdraw her assets
     */
    fun setWhitelist(clientAccount: String, addresses: List<String>) {
        val text = addresses.joinToString()
        logger.info { "Set whitelist $text to $clientAccount by ${registrationConsumer.creator}" }

        ModelUtil.setAccountDetail(
            registrationConsumer,
            clientAccount,
            ETH_WHITE_LIST_KEY,
            text
        )
    }

    /**
     * Add notary to notary list provider. [name] is a string to identify a multisig notary account
     */
    fun addNotary(name: String, address: String) {
        ModelUtil.setAccountDetail(
            notaryListIrohaConsumer,
            accountHelper.notaryListStorageAccount.accountId,
            name,
            address
        )
    }

    /**
     * Add list of [relays].
     * Set relays as details to NotaryAccount from RegistrationAccount
     */
    fun addRelaysToIroha(relays: Map<String, String>) {
        relays.map {
            // Set ethereum wallet as occupied by user id
            ModelUtil.setAccountDetail(
                registrationConsumer,
                accountHelper.notaryAccount.accountId,
                it.key,
                it.value
            )
        }
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
    fun sendRegistrationRequest(
        name: String,
        whitelist: String,
        pubkey: PublicKey,
        port: Int
    ): khttp.responses.Response {
        return khttp.post(
            "http://127.0.0.1:${port}/users",
            data = mapOf("name" to name, "whitelist" to whitelist.trim('[').trim(']'), "pubkey" to pubkey.hex())
        )
    }

    /**
     * Run Ethereum notary process
     */
    fun runEthNotary(
        ethereumPasswords: EthereumPasswords = configHelper.ethPasswordConfig,
        ethNotaryConfig: EthNotaryConfig = configHelper.createEthNotaryConfig()
    ) {
        val name = String.getRandomString(9)
        val address = "http://localhost:${ethNotaryConfig.refund.port}"
        addNotary(name, address)

        val ethCredential =
            WalletUtils.loadCredentials(ethereumPasswords.credentialsPassword, ethNotaryConfig.ethereum.credentialsPath)
        masterContract.addPeer(ethCredential.address).send()

        executeNotary(ethereumPasswords, ethNotaryConfig)

        logger.info { "Notary $name is started on $address" }
    }

    /**
     * Run ethereum registration config
     */
    fun runRegistrationService(registrationConfig: EthRegistrationConfig = ethRegistrationConfig) {
        registration.eth.executeRegistration(registrationConfig, configHelper.ethPasswordConfig)
    }

    /**
     * Run withdrawal service
     */
    fun runEthWithdrawalService(
        withdrawalServiceConfig: WithdrawalServiceConfig = configHelper.createWithdrawalConfig(),
        relayVacuumConfig: RelayVacuumConfig = configHelper.createRelayVacuumConfig()
    ) {
        withdrawalservice.executeWithdrawal(withdrawalServiceConfig, configHelper.ethPasswordConfig, relayVacuumConfig)
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
