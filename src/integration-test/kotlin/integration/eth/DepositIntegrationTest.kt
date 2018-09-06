package integration.eth


import integration.helper.IntegrationHelperUtil
import kotlinx.coroutines.experimental.async
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import sidechain.eth.util.ETH_PRECISION
import util.getRandomString
import java.math.BigDecimal
import java.math.BigInteger

const val WAIT_IROHA_MILLIS = 30_000L

/**
 * Integration tests for deposit case.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DepositIntegrationTest {

    /** Utility functions for integration tests */
    private val integrationHelper = IntegrationHelperUtil()

    /** Testing notrary configuration */
    private val notaryConfig = integrationHelper.createEthNotaryConfig()

    /** Ethereum assetId in Iroha */
    private val etherAssetId = "ether#ethereum"

    init {
        // run notary
        async {
            notary.eth.executeNotary(notaryConfig)
        }
        Thread.sleep(3_000)
    }

    /** Iroha client account */
    private val clientIrohaAccount = String.getRandomString(9)
    private val clientIrohaAccountId = "$clientIrohaAccount@notary"

    /** Ethereum address to transfer to */
    private val relayWallet by lazy {
        integrationHelper.deployRelays(1)
        integrationHelper.registerClient(clientIrohaAccount)
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
        val initialAmount = integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, etherAssetId)
        val amount = BigInteger.valueOf(1_234_000_000_000)
        // send ETH
        integrationHelper.sendEth(amount, relayWallet)
        Thread.sleep(WAIT_IROHA_MILLIS)

        Assertions.assertEquals(
            BigDecimal(amount, ETH_PRECISION).add(BigDecimal(initialAmount)),
            BigDecimal(integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, etherAssetId))
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
        val initialAmount = integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, etherAssetId)
        val zeroAmount = BigInteger.ZERO
        val amount = BigInteger.valueOf(1_234_000_000_000)

        // send 0 ETH
        integrationHelper.sendEth(zeroAmount, relayWallet)
        Thread.sleep(WAIT_IROHA_MILLIS)

        Assertions.assertEquals(
            initialAmount,
            integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, etherAssetId)
        )

        // Send again 1234000000000 Ethereum network
        integrationHelper.sendEth(amount, relayWallet)
        Thread.sleep(WAIT_IROHA_MILLIS)

        Assertions.assertEquals(
            BigDecimal(amount, ETH_PRECISION).add(BigDecimal(initialAmount)),
            BigDecimal(integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, etherAssetId))
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
        val (tokenName, tokenAddress) = integrationHelper.deployRandomERC20Token()
        val assetId = "$tokenName#ethereum"
        val initialAmount = integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId)
        val amount = BigInteger.valueOf(51)

        // send ETH
        integrationHelper.sendERC20Token(tokenAddress, amount, relayWallet)
        Thread.sleep(WAIT_IROHA_MILLIS)
        Assertions.assertEquals(
            BigDecimal(amount).add(BigDecimal(initialAmount)),
            BigDecimal(integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId))
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
        val (tokenName, tokenAddress) = integrationHelper.deployRandomERC20Token()
        val assetId = "$tokenName#ethereum"
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
            BigDecimal(amount).add(BigDecimal(initialAmount)),
            BigDecimal(integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId))
        )
    }
}
