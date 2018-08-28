package integration

import com.github.kittinunf.result.failure
import config.*
import integration.helper.IntegrationHelperUtil
import kotlinx.coroutines.experimental.async
import notary.NotaryConfig
import notary.RefundConfig
import notary.executeNotary
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import sidechain.iroha.IrohaInitialization
import util.getRandomString
import java.math.BigInteger

const val WAIT_IROHA_MILLIS = 30_000L

/**
 * Integration tests for deposit case.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DepositIntegrationTest {

    init {
        IrohaInitialization.loadIrohaLibrary()
            .failure { ex ->
                println(ex)
                System.exit(1)
            }
    }

    private val integrationHelper = IntegrationHelperUtil()

    /** Configurations for tests */
    private val testConfig = loadConfigs("test", TestConfig::class.java, "/test.properties")

    private val notaryConfig = loadConfigs("notary", NotaryConfig::class.java, "/notary.properties")

    init {
        // run notary
        async {
            executeNotary(createConfig())
        }
        Thread.sleep(3_000)
    }

    /** Iroha client account */
    private val clientIrohaAccount = String.getRandomString(9)
    private val clientIrohaAccountId = "$clientIrohaAccount@notary"

    /** Ethereum address to transfer to */
    private val relayWallet by lazy {
        integrationHelper.deployRelays(1)
        integrationHelper.registerRelay(clientIrohaAccount)
    }

    /**
     * Test US-001 Deposit of ETH
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and two ethereum wallets and "fromAddress" with at least
     * 1234000000000 Wei and notary running and user registered with ethereum relayWallet
     * @when "fromAddress" transfers 1234000000000 Wei to "relayWallet"
     * @then Associated Iroha account balance is increased on 1234000000000 Wei
     */
    @Test
    fun depositOfETH() {
        val assetId = "ether#ethereum"
        val initialAmount = integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId)
        val amount = BigInteger.valueOf(1_234_000_000_000)
        // send ETH
        integrationHelper.sendEth(amount, relayWallet)
        Thread.sleep(WAIT_IROHA_MILLIS)
        Assertions.assertEquals(
            initialAmount + amount,
            integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId)
        )
    }

    /**
     * Test US-001 Deposit of ETH
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and two ethereum wallets and "fromAddress" with at least
     * 1234000000000 Wei and notary running
     * @when "fromAddress" transfers 0 Wei to "relayWallet" and then "fromAddress" transfers 1234000000000 Wei
     * to "relayWallet"
     * @then Associated Iroha account balance is increased on 1234000000000 Wei
     */
    @Test
    fun depositZeroETH() {
        val assetId = "ether#ethereum"
        val initialAmount = integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId)
        val zeroAmount = BigInteger.ZERO
        val amount = BigInteger.valueOf(1_234_000_000_000)

        // send 0 ETH
        integrationHelper.sendEth(zeroAmount, relayWallet)
        Thread.sleep(WAIT_IROHA_MILLIS)

        Assertions.assertEquals(initialAmount, integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId))

        // Send again 1234000000000 Ethereum network
        integrationHelper.sendEth(amount, relayWallet)
        Thread.sleep(WAIT_IROHA_MILLIS)

        Assertions.assertEquals(
            initialAmount + amount,
            integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId)
        )
    }

    /**
     * Test US-002 Deposit of ETH
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and two ethereum wallets and "fromAddress" with at least 51 coin
     * (51 coin) and notary running
     * @when "fromAddress" transfers 51 coin to "relayWallet"
     * @then Associated Iroha account balance is increased on 51 coin
     */

    @Test
    fun depositOfERC20() {
        val asset = String.getRandomString(9)
        val assetId = "$asset#ethereum"
        // Deploy ERC20 smart contract
        val tokenAddress = integrationHelper.deployERC20Token(asset)
        val initialAmount = integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId)
        val amount = BigInteger.valueOf(51)

        // send ETH
        integrationHelper.sendERC20Token(tokenAddress, amount, relayWallet)
        Thread.sleep(WAIT_IROHA_MILLIS)
        Assertions.assertEquals(
            initialAmount + amount,
            integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId)
        )
    }

    /**
     * Test US-002 Deposit of ETH
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and two ethereum wallets and "fromAddress" with at least 51 coin
     * (51 coin) and notary running
     * @when "fromAddress" transfers 0 tokens to "relayWallet" and then "fromAddress" transfers 51 coin to "relayWallet"
     * @then Associated Iroha account balance is increased on 51 coin
     */

    @Test
    fun depositZeroOfERC20() {
        val asset = String.getRandomString(9)
        // Deploy ERC20 smart contract
        val tokenAddress = integrationHelper.deployERC20Token(asset)
        val assetId = "$asset#ethereum"
        val initialAmount = integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId)
        val zeroAmount = BigInteger.ZERO
        val amount = BigInteger.valueOf(51)

        // send 0 ERC20
        integrationHelper.sendERC20Token(tokenAddress, zeroAmount, relayWallet)
        Thread.sleep(WAIT_IROHA_MILLIS)

        Assertions.assertEquals(initialAmount, integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId))

        // Send again
        integrationHelper.sendERC20Token(tokenAddress, amount, relayWallet)
        Thread.sleep(WAIT_IROHA_MILLIS)
        Assertions.assertEquals(
            initialAmount + amount,
            integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId)
        )
    }

    private fun createConfig(): NotaryConfig {
        return object : NotaryConfig {
            override val registrationServiceIrohaAccount: String
                get() = integrationHelper.registrationAccount
            override val tokenStorageAccount: String
                get() = integrationHelper.tokenStorageAccount
            override val whitelistSetter: String
                get() = notaryConfig.whitelistSetter
            override val refund: RefundConfig
                get() = notaryConfig.refund
            override val iroha: IrohaConfig
                get() = notaryConfig.iroha
            override val ethereum: EthereumConfig
                get() = testConfig.ethereum
            override val bitcoin: BitcoinConfig
                get() = notaryConfig.bitcoin
        }
    }

}
