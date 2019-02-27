package integration.helper

import com.github.kittinunf.result.success
import config.EthereumPasswords
import config.RMQConfig
import config.getConfigFolder
import config.loadRawConfigs
import jp.co.soramitsu.iroha.java.QueryAPI
import kotlinx.coroutines.runBlocking
import mu.KLogging
import notary.endpoint.eth.EthWhiteListProvider
import notary.eth.EthNotaryConfig
import notary.eth.executeNotary
import provider.eth.ETH_DOMAIN
import provider.eth.EthFreeRelayProvider
import provider.eth.EthRelayProviderIrohaImpl
import provider.eth.EthTokensProviderImpl
import registration.ETH_WHITE_LIST_KEY
import registration.eth.EthRegistrationConfig
import registration.eth.EthRegistrationStrategyImpl
import registration.eth.relay.RelayRegistration
import sidechain.eth.EthChainListener
import sidechain.iroha.CLIENT_DOMAIN
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil
import token.EthTokenInfo
import util.getRandomString
import util.toHexString
import vacuum.RelayVacuumConfig
import withdrawalservice.WithdrawalServiceConfig
import java.math.BigInteger
import java.security.KeyPair

/**
 * Utility class that makes testing more comfortable.
 * Class lazily creates new master contract in Ethereum and master account in Iroha.
 */
class EthIntegrationHelperUtil : IrohaIntegrationHelperUtil() {

    override val accountHelper by lazy { IrohaAccountHelper(irohaAPI) }

    override val configHelper by lazy {
        EthConfigHelper(
            accountHelper,
            relayRegistryContract.contractAddress,
            masterContract.contractAddress,
            contractTestHelper.relayImplementation.contractAddress
        )
    }

    val ethRegistrationConfig by lazy { configHelper.createEthRegistrationConfig(testConfig.ethereum) }

    /** Ethereum utils */
    private val contractTestHelper by lazy { ContractTestHelper() }

    val ethListener = EthChainListener(
        contractTestHelper.deployHelper.web3,
        BigInteger.valueOf(testConfig.ethereum.confirmationPeriod)
    )

    private val tokenProviderIrohaConsumer by lazy {
        IrohaConsumerImpl(accountHelper.tokenSetterAccount, irohaAPI)
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

    /** Provider that is used to store/fetch tokens*/
    val ethTokensProvider by lazy {
        EthTokensProviderImpl(
            queryAPI,
            accountHelper.tokenStorageAccount.accountId,
            accountHelper.tokenSetterAccount.accountId
        )
    }

    private val registrationQueryAPI =
        QueryAPI(irohaAPI, accountHelper.registrationAccount.accountId, accountHelper.registrationAccount.keyPair)

    /** Provider that is used to get free registered relays*/
    private val ethFreeRelayProvider by lazy {
        EthFreeRelayProvider(
            registrationQueryAPI,
            accountHelper.notaryAccount.accountId,
            accountHelper.registrationAccount.accountId
        )
    }

    /** Provider of ETH wallets created by registrationAccount*/
    private val ethRelayProvider by lazy {
        EthRelayProviderIrohaImpl(
            registrationQueryAPI,
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

    private val relayRegistration by lazy {
        RelayRegistration(
            ethFreeRelayProvider,
            configHelper.createRelayRegistrationConfig(),
            accountHelper.registrationAccount,
            irohaAPI,
            configHelper.ethPasswordConfig
        )
    }

    private val whitelistProvider by lazy {
        EthWhiteListProvider(
            ethRegistrationConfig.registrationCredential.accountId, queryAPI
        )
    }

    /**
     * Get address of first free relay.
     */
    fun getFreeRelay(): String {
        return ethFreeRelayProvider.getRelay().get()
    }

    /**
     * Get relay address of an account.
     */
    fun getRelayByAccount(clientId: String): String {
        return ethRelayProvider.getRelay(clientId).get()
    }

    /**
     * Check if address is in whitelist of a user.
     */
    fun isWhitelisted(clientId: String, address: String): Boolean {
        return whitelistProvider.checkWithdrawalAddress(clientId, address).get()
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
    fun deployRandomERC20Token(precision: Int = 0): Pair<EthTokenInfo, String> {
        val name = String.getRandomString(5)
        return Pair(EthTokenInfo(name, ETH_DOMAIN, precision), deployERC20Token(name, precision))
    }

    /**
     * Deploy ERC20 token and register it to the notary system:
     * - create asset in Iroha
     * - add to Token provider service
     * - add to master contract
     * @return token name in iroha and address of ERC20 smart contract
     */
    fun deployERC20Token(name: String, precision: Int): String {
        logger.info { "create $name ERC20 token" }
        val tokenAddress = contractTestHelper.deployHelper.deployERC20TokenSmartContract().contractAddress
        addERC20Token(tokenAddress, EthTokenInfo(name, ETH_DOMAIN, precision))
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
     * @param tokenAddress - token ERC20 smart contract address
     * @param tokenInfo - token info
     */
    fun addERC20Token(tokenAddress: String, tokenInfo: EthTokenInfo) {
        ModelUtil.createAsset(irohaConsumer, tokenInfo.name, tokenInfo.domain, tokenInfo.precision)
        ModelUtil.setAccountDetail(
            tokenProviderIrohaConsumer,
            accountHelper.tokenStorageAccount.accountId,
            tokenAddress,
            "${tokenInfo.name}#${tokenInfo.domain}"
        ).success {
            logger.info { "token ${tokenInfo.name}#${tokenInfo.domain} was added to ${accountHelper.tokenStorageAccount} by ${tokenProviderIrohaConsumer.creator}" }
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
     * Deploys relay contracts in Ethereum network
     */
    fun deployRelays(relaysToDeploy: Int) {
        relayRegistration.deploy(
            relaysToDeploy,
            contractTestHelper.relayImplementation.contractAddress,
            masterContract.contractAddress
        )
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
        keypair: KeyPair = ModelUtil.generateKeypair()
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
        keypair: KeyPair = ModelUtil.generateKeypair()
    ): String {
        ethRegistrationStrategy.register(name, CLIENT_DOMAIN, whitelist, keypair.public.toHexString())
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

    /**
     * Returns wallets registered by master account in Iroha
     */
    fun getRegisteredEthWallets(): Set<String> = ethRelayProvider.getRelays().get().keys

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
     * Send HTTP POST request to registration service to register user
     * @param name - user name
     * @param pubkey - user public key in hexidecimal representation
     * @param port - port of registration service
     */
    fun sendRegistrationRequest(
        name: String,
        whitelist: String,
        pubkey: String,
        port: Int
    ): khttp.responses.Response {
        return khttp.post(
            "http://127.0.0.1:${port}/users",
            data = mapOf(
                "name" to name,
                "whitelist" to whitelist.trim('[').trim(']'),
                "pubkey" to pubkey
            )
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
        relayVacuumConfig: RelayVacuumConfig = configHelper.createRelayVacuumConfig(),
        rmqConfig: RMQConfig = loadRawConfigs("rmq", RMQConfig::class.java, "${getConfigFolder()}/rmq.properties")
    ) {
        withdrawalservice.executeWithdrawal(
            withdrawalServiceConfig,
            configHelper.ethPasswordConfig,
            relayVacuumConfig,
            rmqConfig
        )
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
