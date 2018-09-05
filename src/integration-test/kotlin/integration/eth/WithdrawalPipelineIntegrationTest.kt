package integration.eth

import integration.helper.IntegrationHelperUtil
import jp.co.soramitsu.iroha.Keypair
import jp.co.soramitsu.iroha.ModelCrypto
import kotlinx.coroutines.experimental.async
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import util.getRandomString
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
    private val notaryConfig = integrationHelper.createEthNotaryConfig()

    /** Refund endpoint address */
    private val refundAddress = "http://localhost:${notaryConfig.refund.port}"

    /** Test Registration configuration */
    private val registrationConfig = integrationHelper.createEthRegistrationConfig()

    /** Test Withdrawal configuration */
    private val withdrawalServiceConfig = integrationHelper.createWithdrawalConfig()

    /** Ethereum password configs */
    private val passwordConfig = integrationHelper.configHelper.ethPasswordConfig

    /** Ethereum test address where we want to withdraw to */
    private val toAddress = integrationHelper.configHelper.testConfig.ethTestAccount

    /** Notary account in Iroha */
    private val notaryAccount = notaryConfig.iroha.creator

    init {
        integrationHelper.runEthNotary(notaryConfig)
        async {
            registration.eth.executeRegistration(registrationConfig)
        }
        async {
            withdrawalservice.executeWithdrawal(withdrawalServiceConfig, passwordConfig)
        }
        Thread.sleep(3_000)
    }

    lateinit var clientName: String
    lateinit var clientId: String
    lateinit var keypair: Keypair

    @BeforeEach
    fun setup() {
        // generate client name and key
        clientName = String.getRandomString(9)
        clientId = "$clientName@notary"
        keypair = ModelCrypto().generateKeypair()
    }

    /**
     * Full withdrawal pipeline test
     * @given iroha and withdrawal services are running, free relays available, user account has 125 Wei in Iroha
     * @when user transfers 125 Wei to Iroha master account
     * @then balance of user's wallet in Ethereum increased by 125 Wei
     */
    @Test
    fun testFullWithdrawalPipeline() {
        // deploy free relay
        integrationHelper.deployRelays(1)

        // make sure master has enough assets
        integrationHelper.sendEth(BigInteger.valueOf(125), integrationHelper.masterContract.contractAddress)

        // register client
        val res = integrationHelper.sendRegistrationRequest(clientName, keypair.publicKey(), registrationConfig.port)
        Assertions.assertEquals(200, res.statusCode)

        integrationHelper.setWhitelist(clientId, listOf(toAddress))

        val initialBalance = integrationHelper.getEthBalance(toAddress)

        val amount = "125"
        val assetId = "ether#ethereum"

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
            initialBalance + BigInteger.valueOf(amount.toLong()),
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
        // deploy free relay
        integrationHelper.deployRelays(1)

        // create ERC20 token and transfer to master
        val (assetName, tokenAddress) = integrationHelper.deployRandomERC20Token()

        integrationHelper.sendERC20Token(
            tokenAddress,
            BigInteger.valueOf(125),
            integrationHelper.masterContract.contractAddress
        )

        val amount = "125"
        val domain = "ethereum"
        val assetId = "$assetName#$domain"

        // register client
        val res = integrationHelper.sendRegistrationRequest(clientName, keypair.publicKey(), registrationConfig.port)
        Assertions.assertEquals(200, res.statusCode)

        integrationHelper.setWhitelist(clientId, listOf(toAddress))

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
            initialBalance + BigInteger.valueOf(amount.toLong()),
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
        integrationHelper.registerClient(clientName, keypair)

        integrationHelper.setWhitelist(clientId, listOf(toAddress, "0xSOME_ANOTHER_ETH_ADDRESS"))

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
        integrationHelper.registerClient(clientName, keypair)

        val withdrawalEthAddress = "some_address"

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
        integrationHelper.registerClient(clientName, keypair)
        integrationHelper.setWhitelist(clientId, listOf("0xANOTHER_ETH_ADDRESS"))

        val withdrawalEthAddress = "some_address"

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
