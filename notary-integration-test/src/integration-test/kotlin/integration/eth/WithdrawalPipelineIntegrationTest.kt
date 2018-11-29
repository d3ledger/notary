package integration.eth

import integration.helper.IntegrationHelperUtil
import jp.co.soramitsu.iroha.Keypair
import jp.co.soramitsu.iroha.ModelCrypto
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.jupiter.api.*
import provider.eth.ETH_PRECISION
import sidechain.iroha.CLIENT_DOMAIN
import util.getRandomString
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertEquals

/**
 * Integration tests for withdrawal service.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WithdrawalPipelineIntegrationTest {

    /** Integration tests util */
    private val integrationHelper = IntegrationHelperUtil()

    /** Test Notary configuration */
    private val notaryConfig = integrationHelper.configHelper.createEthNotaryConfig()

    /** Refund endpoint address */
    private val refundAddress = "http://localhost:${notaryConfig.refund.port}"

    /** Test Registration configuration */
    private val registrationConfig = integrationHelper.ethRegistrationConfig

    /** Ethereum test address where we want to withdraw to */
    private val toAddress = integrationHelper.configHelper.testConfig.ethTestAccount

    /** Notary account in Iroha */
    private val notaryAccount = integrationHelper.accountHelper.notaryAccount.accountId

    private val registrationService: Job

    private val withdrawalService: Job

    init {
        integrationHelper.runEthNotary(ethNotaryConfig = notaryConfig)
        registrationService = GlobalScope.launch {
            integrationHelper.runRegistrationService(registrationConfig)
        }
        withdrawalService = GlobalScope.launch {
            integrationHelper.runEthWithdrawalService()
        }

        integrationHelper.lockEthMasterSmartcontract()
    }

    lateinit var clientName: String
    lateinit var clientId: String
    lateinit var keypair: Keypair

    @BeforeEach
    fun setup() {
        // generate client name and key
        clientName = String.getRandomString(9)
        clientId = "$clientName@$CLIENT_DOMAIN"
        keypair = ModelCrypto().generateKeypair()
    }

    @AfterAll
    fun dropDown() {
        integrationHelper.close()
        registrationService.cancel()
        withdrawalService.cancel()
    }

    /**
     * Full withdrawal pipeline test
     * @given iroha and withdrawal services are running, free relays available, user account has 125 Wei in Iroha
     * @when user transfers 125 Wei to Iroha master account
     * @then balance of user's wallet in Ethereum increased by 125 Wei
     */
    @Test
    fun testFullWithdrawalPipeline() {
        val amount = BigInteger.valueOf(1251400000000)

        // deploy free relay
        integrationHelper.deployRelays(1)

        // make sure master has enough assets
        integrationHelper.sendEth(amount, integrationHelper.masterContract.contractAddress)

        // register client
        val res = integrationHelper.sendRegistrationRequest(
            clientName,
            listOf(toAddress).toString(),
            keypair.publicKey(),
            registrationConfig.port
        )
        Assertions.assertEquals(200, res.statusCode)

        val initialBalance = integrationHelper.getEthBalance(toAddress)
        val decimalAmount = BigDecimal(amount, ETH_PRECISION.toInt()).toPlainString()
        val assetId = "ether#ethereum"

        // add assets to user
        integrationHelper.addIrohaAssetTo(clientId, assetId, decimalAmount)

        // transfer assets from user to notary master account
        integrationHelper.transferAssetIrohaFromClient(
            clientId,
            keypair,
            clientId,
            notaryAccount,
            assetId,
            toAddress,
            decimalAmount
        )
        Thread.sleep(15_000)

        Assertions.assertEquals(
            initialBalance.add(amount),
            integrationHelper.getEthBalance(toAddress)
        )
    }

    /**
     * Full withdrawal pipeline test for ERC20 token
     * @given iroha and withdrawal services are running, free relays available, user account has 125 OMG tokens in Iroha
     * @when user transfers 125 OMG to Iroha master account
     * @then balance of user's wallet in Ethereum increased by 125 OMG
     */
    @Test
    fun testFullWithdrawalPipelineErc20() {
        val precision: Short = 2

        // deploy free relay
        integrationHelper.deployRelays(1)

        // create ERC20 token and transfer to master
        val (assetInfo, tokenAddress) = integrationHelper.deployRandomERC20Token(precision)

        integrationHelper.sendERC20Token(
            tokenAddress,
            BigInteger.valueOf(125),
            integrationHelper.masterContract.contractAddress
        )

        val amount = "1.25"
        val domain = "ethereum"
        val assetId = "${assetInfo.name}#$domain"

        // register client
        val res = integrationHelper.sendRegistrationRequest(
            clientName,
            listOf(toAddress).toString(),
            keypair.publicKey(),
            registrationConfig.port
        )
        Assertions.assertEquals(200, res.statusCode)

        val initialBalance = integrationHelper.getERC20TokenBalance(tokenAddress, toAddress)

        // add assets to user
        integrationHelper.addIrohaAssetTo(clientId, assetId, amount)

        // transfer assets from user to notary master account
        integrationHelper.transferAssetIrohaFromClient(
            clientId,
            keypair,
            clientId,
            notaryAccount,
            assetId,
            toAddress,
            amount
        )
        Thread.sleep(15_000)

        Assertions.assertEquals(
            initialBalance.add(BigDecimal(amount).scaleByPowerOfTen(precision.toInt()).toBigInteger()),
            integrationHelper.getERC20TokenBalance(tokenAddress, toAddress)
        )
    }

    /**
     * Try to withdraw to address in whitelist
     * @given A notary client and withdrawal address. Withdrawal address is in whitelist
     * @when client initiates withdrawal
     * @then notary approves the withdrawal
     */
    @Test
    fun testWithdrawInWhitelist() {
        integrationHelper.registerClient(clientName, listOf(toAddress, "0x123"), keypair)

        val amount = "125"
        val assetId = "ether#ethereum"

        // add assets to user
        integrationHelper.addIrohaAssetTo(clientId, assetId, amount)

        // make transfer to notaryAccount to initiate withdrawal
        val hash = integrationHelper.transferAssetIrohaFromClient(
            clientId,
            keypair,
            clientId,
            notaryAccount,
            assetId,
            toAddress,
            amount
        )

        // try get proof from peer
        val res = khttp.get("$refundAddress/eth/$hash")

        assertEquals(200, res.statusCode)
    }

    /**
     * Try to withdraw to address with empty whitelist
     * @given A notary client and withdrawal address. Whitelist is empty
     * @when client initiates withdrawal
     * @then withdrawal is allowed by notary
     */
    @Test
    fun testWithdrawEmptyWhitelist() {
        // TODO: D3-417 Web3j cannot pass an empty list of addresses to the smart contract.
        integrationHelper.registerClient(clientName, listOf(), keypair)

        val withdrawalEthAddress = "0x123"

        val amount = "125"
        val assetId = "ether#ethereum"

        // add assets to user
        integrationHelper.addIrohaAssetTo(clientId, assetId, amount)

        // make transfer trx
        val hash = integrationHelper.transferAssetIrohaFromClient(
            clientId,
            keypair,
            clientId,
            notaryAccount,
            assetId,
            withdrawalEthAddress,
            amount
        )

        val res = khttp.get("$refundAddress/eth/$hash")

        assertEquals(200, res.statusCode)
    }

    /**
     * Try to withdraw to address not in whitelist
     * @given A notary client and withdrawal address. Withdrawal address is absent in whitelist
     * @when client initiates withdrawal
     * @then withdrawal is disallowed by notary
     */
    @Test
    fun testWithdrawNotInWhitelist() {
        integrationHelper.registerClient(clientName, listOf("0x123"), keypair)
        integrationHelper.setWhitelist(clientId, listOf("0x123"))

        val withdrawalEthAddress = "0x321"

        val amount = "125"
        val assetId = "ether#ethereum"

        // add assets to user
        integrationHelper.addIrohaAssetTo(clientId, assetId, amount)

        // make transfer trx
        val hash = integrationHelper.transferAssetIrohaFromClient(
            clientId,
            keypair,
            clientId,
            notaryAccount,
            assetId,
            withdrawalEthAddress,
            amount
        )

        val res = khttp.get("$refundAddress/eth/$hash")

        assertEquals(400, res.statusCode)
        assertEquals(
            "notary.endpoint.eth.NotaryException: ${withdrawalEthAddress} not in whitelist",
            res.jsonObject.get("reason")
        )
    }
}
