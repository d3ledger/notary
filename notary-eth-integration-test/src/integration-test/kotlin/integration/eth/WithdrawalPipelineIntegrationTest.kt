package integration.eth

import integration.helper.EthIntegrationHelperUtil
import integration.helper.IrohaConfigHelper
import integration.registration.RegistrationServiceTestEnvironment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.jupiter.api.*
import provider.eth.ETH_PRECISION
import sidechain.iroha.CLIENT_DOMAIN
import sidechain.iroha.util.ModelUtil
import util.getRandomString
import util.toHexString
import java.math.BigDecimal
import java.math.BigInteger
import java.security.KeyPair
import java.time.Duration
import kotlin.test.assertEquals

/**
 * Integration tests for withdrawal service.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WithdrawalPipelineIntegrationTest {

    /** Integration tests util */
    private val integrationHelper = EthIntegrationHelperUtil()

    /** Test Notary configuration */
    private val notaryConfig = integrationHelper.configHelper.createEthNotaryConfig()

    /** Refund endpoint address */
    private val refundAddress = "http://localhost:${notaryConfig.refund.port}"

    private val registrationTestEnvironment = RegistrationServiceTestEnvironment(integrationHelper)

    /** Test Registration configuration */
    private val registrationConfig = registrationTestEnvironment.registrationConfig

    /** Test EthRegistration configuration */
    private val ethRegistrationConfig = integrationHelper.ethRegistrationConfig

    /** Ethereum test address where we want to withdraw to */
    private val toAddress = integrationHelper.configHelper.testConfig.ethTestAccount

    /** Notary account in Iroha */
    private val notaryAccount = integrationHelper.accountHelper.notaryAccount.accountId

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    private val ethRegistrationService: Job

    init {

        registrationTestEnvironment.registrationInitialization.init()
        ethRegistrationService = GlobalScope.launch {
            integrationHelper.runEthRegistrationService(ethRegistrationConfig)
        }
        integrationHelper.runEthNotary(ethNotaryConfig = notaryConfig)
        registrationService = GlobalScope.launch {
            integrationHelper.runRegistrationService(registrationConfig)
        }
        integrationHelper.runEthWithdrawalService()
    }

    lateinit var clientName: String
    lateinit var clientId: String
    lateinit var keypair: KeyPair

    @BeforeEach
    fun setup() {
        // generate client name and key
        clientName = String.getRandomString(9)
        clientId = "$clientName@$CLIENT_DOMAIN"
        keypair = ModelUtil.generateKeypair()
    }

    @AfterAll
    fun dropDown() {
        registrationTestEnvironment.close()
        integrationHelper.close()
        registrationService.cancel()
        integrationHelper.stopEthWithdrawal()
    }

    /**
     * Full withdrawal pipeline test
     * @given iroha and withdrawal services are running, free relays available, user account has 125 Wei in Iroha
     * @when user transfers 125 Wei to Iroha master account
     * @then balance of user's wallet in Ethereum increased by 125 Wei
     */
    @Test
    fun testFullWithdrawalPipeline() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val amount = BigInteger.valueOf(1251400000000)

            // deploy free relay
            integrationHelper.deployRelays(1)

            // make sure master has enough assets
            integrationHelper.sendEth(amount, integrationHelper.masterContract.contractAddress)

            // register client in Iroha
            var res = integrationHelper.sendRegistrationRequest(
                clientName,
                listOf(toAddress).toString(),
                keypair.public.toHexString(),
                registrationConfig.port
            )
            Assertions.assertEquals(200, res.statusCode)

            // register client in Ethereum
            res = integrationHelper.sendRegistrationRequest(
                clientName,
                listOf(toAddress).toString(),
                keypair.public.toHexString(),
                ethRegistrationConfig.port
            )
            Assertions.assertEquals(200, res.statusCode)

            val initialBalance = integrationHelper.getEthBalance(toAddress)
            val decimalAmount = BigDecimal(amount, ETH_PRECISION)
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
                decimalAmount.toPlainString()
            )
            Thread.sleep(15_000)

            Assertions.assertEquals(
                initialBalance.add(amount),
                integrationHelper.getEthBalance(toAddress)
            )
        }
    }

    /**
     * Full withdrawal pipeline test for ERC20 token
     * @given iroha and withdrawal services are running, free relays available, user account has 125 OMG tokens in Iroha
     * @when user transfers 125 OMG to Iroha master account
     * @then balance of user's wallet in Ethereum increased by 125 OMG
     */
    @Test
    fun testFullWithdrawalPipelineErc20() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val precision = 2

            // deploy free relay
            integrationHelper.deployRelays(1)

            // create ERC20 token and transfer to master
            val (assetInfo, tokenAddress) = integrationHelper.deployRandomERC20Token(precision)

            val bigIntegerValue = BigInteger.valueOf(125)
            integrationHelper.sendERC20Token(
                tokenAddress,
                bigIntegerValue,
                integrationHelper.masterContract.contractAddress
            )

            val amount = BigDecimal(1.25)
            val assetId = "${assetInfo.name}#${assetInfo.domain}"

            // register client in Iroha
            var res = integrationHelper.sendRegistrationRequest(
                clientName,
                listOf(toAddress).toString(),
                keypair.public.toHexString(),
                registrationConfig.port
            )
            Assertions.assertEquals(200, res.statusCode)

            // register client in Ethereum
            res = integrationHelper.sendRegistrationRequest(
                clientName,
                listOf(toAddress).toString(),
                keypair.public.toHexString(),
                ethRegistrationConfig.port
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
                amount.toPlainString()
            )
            Thread.sleep(15_000)

            Assertions.assertEquals(
                initialBalance.add(bigIntegerValue),
                integrationHelper.getERC20TokenBalance(tokenAddress, toAddress)
            )
        }
    }

    /**
     * Try to withdraw to address in whitelist
     * @given A notary client and withdrawal address. Withdrawal address is in whitelist
     * @when client initiates withdrawal
     * @then notary approves the withdrawal
     */
    @Test
    fun testWithdrawInWhitelist() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.registerClient(clientName, CLIENT_DOMAIN, listOf(toAddress, "0x123"), keypair)

            val amount = BigDecimal(125)
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
                amount.toPlainString()
            )

            // TODO: Added because of statuses bug in Iroha
            Thread.sleep(5000)

            // try get proof from peer
            val res = khttp.get("$refundAddress/eth/$hash")

            assertEquals(200, res.statusCode)
        }
    }

    /**
     * Try to withdraw to address with empty whitelist
     * @given A notary client and withdrawal address. Whitelist is empty
     * @when client initiates withdrawal
     * @then withdrawal is allowed by notary
     */
    @Test
    fun testWithdrawEmptyWhitelist() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            // TODO: D3-417 Web3j cannot pass an empty list of addresses to the smart contract.
            integrationHelper.registerClient(clientName, CLIENT_DOMAIN, listOf(), keypair)

            val withdrawalEthAddress = "0x123"

            val amount = BigDecimal(125)
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
                amount.toPlainString()
            )

            // TODO: Added because of statuses bug in Iroha
            Thread.sleep(5000)

            val res = khttp.get("$refundAddress/eth/$hash")

            assertEquals(200, res.statusCode)
        }
    }

    /**
     * Try to withdraw to address not in whitelist
     * @given A notary client and withdrawal address. Withdrawal address is absent in whitelist
     * @when client initiates withdrawal
     * @then withdrawal is disallowed by notary
     */
    @Test
    fun testWithdrawNotInWhitelist() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.registerClient(clientName, CLIENT_DOMAIN, listOf("0x123"), keypair)
            integrationHelper.setWhitelist(clientId, listOf("0x123"))

            val withdrawalEthAddress = "0x321"

            val amount = BigDecimal(125)
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
                amount.toPlainString()
            )

            // TODO: Added because of statuses bug in Iroha
            Thread.sleep(5000)

            val res = khttp.get("$refundAddress/eth/$hash")

            assertEquals(400, res.statusCode)
            assertEquals(
                "notary.endpoint.eth.NotaryException: ${withdrawalEthAddress} not in whitelist",
                res.jsonObject.get("reason")
            )
        }
    }
}
