package integration

import com.github.kittinunf.result.failure
import config.EthereumPasswords
import config.TestConfig
import config.loadConfigs
import integration.helper.IntegrationHelperUtil
import jp.co.soramitsu.iroha.ModelCrypto
import kotlinx.coroutines.experimental.async
import notary.NotaryConfig
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.web3j.protocol.core.DefaultBlockParameterName
import sidechain.eth.util.DeployHelper
import sidechain.iroha.IrohaInitialization
import util.getRandomString
import java.math.BigInteger
import kotlin.test.assertEquals

/**
 * Integration tests for withdrawal service.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WithdrawalPipelineIntegrationTest {

    init {
        IrohaInitialization.loadIrohaLibrary()
            .failure { ex ->
                ex.printStackTrace()
                System.exit(1)
            }

        async {
            notary.main(emptyArray())
        }
        async {
            registration.main(emptyArray())
        }
        async {
            withdrawalservice.main(emptyArray())
        }
        Thread.sleep(3_000)
    }

    /** Configuration for notary instance */
    private val notaryConfig = loadConfigs("notary", NotaryConfig::class.java, "/notary.properties")

    /** Configurations for tests */
    private val testConfig = loadConfigs("test", TestConfig::class.java, "/test.properties")

    /** Ethereum password configs */
    private val passwordConfig = loadConfigs("test", EthereumPasswords::class.java, "/ethereum_password.properties")

    /** Refund endpoint address */
    val refundAddress = "http://localhost:${notaryConfig.refund.port}"

    /** Ethereum utils */
    private val deployHelper = DeployHelper(testConfig.ethereum, passwordConfig)

    /** Integration tests util */
    private val integrationHelper = IntegrationHelperUtil()

    /** Ethereum test address where we want to withdraw to */
    private val toAddress = testConfig.ethTestAccount

    /** Registration service port */
    private val registrationServicePort = 8083

    /** Notary account in Iroha */
    val notaryAccount = testConfig.notaryIrohaAccount

    /**
     * Full withdrawal pipeline test
     * @given iroha and withdrawal services are running, free relays available, user account has 125 Wei in Iroha
     * @when user transfers 125 Wei to Iroha master account
     * @then balance of user's wallet in Ethereum increased by 125 Wei
     */
    @Test
    fun testFullWithdrawalPipeline() {
        // generate client name and key
        val name = String.getRandomString(9)
        val clientId = "$name@notary"
        val keypair = ModelCrypto().generateKeypair()

        // deploy free relay
        integrationHelper.deployRelays(1)

        // make sure master has enough assets
        integrationHelper.sendEth(BigInteger.valueOf(125), integrationHelper.masterContract.contractAddress)

        // register client
        val res = khttp.post(
            "http://127.0.0.1:$registrationServicePort/users",
            data = mapOf("name" to name, "pubkey" to keypair.publicKey().hex())
        )
        Assertions.assertEquals(200, res.statusCode)

        integrationHelper.setWhitelist(clientId, listOf(toAddress))

        val initialBalance = deployHelper.web3.ethGetBalance(toAddress, DefaultBlockParameterName.LATEST).send().balance

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
            deployHelper.web3.ethGetBalance(toAddress, DefaultBlockParameterName.LATEST).send().balance
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
        val name = String.getRandomString(9)
        val fullName = "$name@notary"
        val keypair = ModelCrypto().generateKeypair()

        // deploy free relay
        integrationHelper.deployRelays(1)

        // create ERC20 token and transfer to master
        integrationHelper.deployERC20Token()
        val (assetName, token) = integrationHelper.tokenContracts.entries.first()

        token.transfer(integrationHelper.masterContract.contractAddress, BigInteger.valueOf(125)).send()

        val amount = "125"
        val domain = "ethereum"
        val assetId = "$assetName#$domain"

        // register user
        val res = khttp.post(
            "http://127.0.0.1:$registrationServicePort/users",
            data = mapOf("name" to name, "pubkey" to keypair.publicKey().hex())
        )
        Assertions.assertEquals(200, res.statusCode)

        integrationHelper.setWhitelist(fullName, listOf(toAddress))

        val initialBalance = token.balanceOf(toAddress).send()

        // add assets to user
        integrationHelper.addIrohaAssetTo(fullName, assetId, amount)

        // transfer assets from user to notary master account
        integrationHelper.transferAssetIrohaFromClient(
            fullName,
            keypair,
            fullName,
            notaryAccount,
            assetId,
            toAddress,
            amount
        )
        Thread.sleep(15_000)

        Assertions.assertEquals(initialBalance + BigInteger.valueOf(amount.toLong()), token.balanceOf(toAddress).send())
    }

    /**
     * Try to withdraw to address in whitelist
     * @given A notary client and withdrawal address. Withdrawal address is in whitelist
     * @when client initiates withdrawal
     * @then notary approves the withdrawal
     */
    @Test
    fun testWithdrawInWhitelist() {
        // create client
        val clientId = integrationHelper.registerClient()
        val clientKeypair = integrationHelper.irohaKeyPair

        integrationHelper.setWhitelist(clientId, listOf(toAddress, "0xSOME_ANOTHER_ETH_ADDRESS"))

        val amount = "125"
        val assetId = "ether#ethereum"

        // add assets to user
        integrationHelper.addIrohaAssetTo(clientId, assetId, amount)

        // make transfer to notaryAccount to initiate withdrawal
        val hash = integrationHelper.transferAssetIrohaFromClient(
            clientId,
            clientKeypair,
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
     * Try to withdraw to address not in whitelist
     * @given A notary client and withdrawal address. Withdrawal address is absent in whitelist
     * @when client initiates withdrawal
     * @then withdrawal is prevented by notary
     */
    @Test
    fun testWithdrawNotInWhitelist() {
        val fakeEthAddress = "fake_address"

        // create client
        val clientId = integrationHelper.registerClient()
        val clientKeypair = integrationHelper.irohaKeyPair

        val amount = "125"
        val assetId = "ether#ethereum"

        // add assets to user
        integrationHelper.addIrohaAssetTo(clientId, assetId, amount)

        // make transfer trx
        val hash = integrationHelper.transferAssetIrohaFromClient(
            clientId,
            clientKeypair,
            clientId,
            notaryAccount,
            assetId,
            fakeEthAddress,
            amount
        )

        val res = khttp.get("$refundAddress/eth/$hash")

        assertEquals(400, res.statusCode)
        assertEquals("notary.endpoint.eth.NotaryException: fake_address not in whitelist", res.jsonObject.get("reason"))
    }

}
