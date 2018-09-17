package integration.eth

import integration.helper.IntegrationHelperUtil
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import sidechain.eth.util.ETH_PRECISION
import sidechain.iroha.util.ModelUtil
import util.getRandomString
import java.math.BigDecimal
import java.math.BigInteger


/**
 * Integration tests for deposit case.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DepositIntegrationTest {
    /** Utility functions for integration tests */
    private val integrationHelper = IntegrationHelperUtil()

    /** Ethereum assetId in Iroha */
    private val etherAssetId = "ether#ethereum"

    init {
        // run notary
        integrationHelper.runEthNotary()
    }

    /** Iroha client account */
    private val clientIrohaAccount = String.getRandomString(9)
    private val clientIrohaAccountId = "$clientIrohaAccount@notary"

    /** Ethereum address to transfer to */
    private val relayWallet = registerRelay()

    private fun registerRelay(): String {
        integrationHelper.deployRelays(1)
        return integrationHelper.registerClient(clientIrohaAccount)
    }

    /** Path to public key of 2nd instance of notary */
    private val pubkeyPath = "deploy/iroha/keys/notary2@notary.pub"

    /** Path to private key of 2nd instance of notary */
    private val privkeyPath = "deploy/iroha/keys/notary2@notary.priv"


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
        integrationHelper.waitOneIrohaBlock()

        Assertions.assertEquals(
            BigDecimal(amount, ETH_PRECISION.toInt()).add(BigDecimal(initialAmount)),
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

        Assertions.assertEquals(
            initialAmount,
            integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, etherAssetId)
        )

        // Send again 1234000000000 Ethereum network
        integrationHelper.sendEth(amount, relayWallet)
        integrationHelper.waitOneIrohaBlock()

        Assertions.assertEquals(
            BigDecimal(amount, ETH_PRECISION.toInt()).add(BigDecimal(initialAmount)),
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
        val (tokenInfo, tokenAddress) = integrationHelper.deployRandomERC20Token(2)
        val assetId = "${tokenInfo.name}#ethereum"
        val initialAmount = integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId)
        val amount = BigInteger.valueOf(51)

        // send ETH
        integrationHelper.sendERC20Token(tokenAddress, amount, relayWallet)
        integrationHelper.waitOneIrohaBlock()

        Assertions.assertEquals(
            BigDecimal(amount, tokenInfo.precision.toInt()).add(BigDecimal(initialAmount)),
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
        val (tokenInfo, tokenAddress) = integrationHelper.deployRandomERC20Token(2)
        val assetId = "${tokenInfo.name}#ethereum"
        val initialAmount = integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId)
        val zeroAmount = BigInteger.ZERO
        val amount = BigInteger.valueOf(51)

        // send 0 ERC20
        integrationHelper.sendERC20Token(tokenAddress, zeroAmount, relayWallet)

        Assertions.assertEquals(initialAmount, integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId))

        // Send again
        integrationHelper.sendERC20Token(tokenAddress, amount, relayWallet)
        integrationHelper.waitOneIrohaBlock()

        Assertions.assertEquals(
            BigDecimal(amount, tokenInfo.precision.toInt()).add(BigDecimal(initialAmount)),
            BigDecimal(integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId))
        )
    }

    /**
     * Test US-001 Deposit of ETH with multiple notaries
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and two ethereum wallets and "fromAddress" with at least
     * 1234000000000 Wei and 2 instances of notary running
     * @when "fromAddress" transfers 0 Wei to "relayWallet" and then "fromAddress" transfers 1234000000000 Wei
     * to "relayWallet"
     * @then Associated Iroha account balance is increased on 1234000000000 Wei
     */
    @Test
    fun depositMultisig() {
        val irohaConfig =
            integrationHelper.configHelper.createIrohaConfig(pubkeyPath = pubkeyPath, privkeyPath = privkeyPath)
        val notaryConfig = integrationHelper.configHelper.createEthNotaryConfig(irohaConfig)

        val keypair = ModelUtil.loadKeypair(pubkeyPath, privkeyPath).get()

        integrationHelper.accountHelper.addNotarySignatory(keypair)

        integrationHelper.runEthNotary(notaryConfig)

        val initialAmount = integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, etherAssetId)
        val amount = BigInteger.valueOf(1_234_000_000_000)
        // send ETH
        integrationHelper.sendEth(amount, relayWallet)
        integrationHelper.waitOneIrohaBlock()

        Assertions.assertEquals(
            BigDecimal(amount, ETH_PRECISION.toInt()).add(BigDecimal(initialAmount)),
            BigDecimal(integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, etherAssetId))
        )
    }

    /**
     * Test US-002 Deposit of ETH token with multiple notaries
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and two ethereum wallets and "fromAddress" with at least 51 coin
     * (51 coin) and 2 notaries running
     * @when "fromAddress" transfers 0 tokens to "relayWallet" and then "fromAddress" transfers 51 coin to "relayWallet"
     * @then Associated Iroha account balance is increased on 51 coin
     */
    @Test
    fun depositMultisigERC20() {
        val (tokenInfo, tokenAddress) = integrationHelper.deployRandomERC20Token(2)
        val assetId = "${tokenInfo.name}#ethereum"
        val initialAmount = integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId)
        val amount = BigInteger.valueOf(51)

        val irohaConfig =
            integrationHelper.configHelper.createIrohaConfig(pubkeyPath = pubkeyPath, privkeyPath = privkeyPath)
        val notaryConfig = integrationHelper.configHelper.createEthNotaryConfig(irohaConfig)

        val keypair = ModelUtil.loadKeypair(pubkeyPath, privkeyPath).get()

        integrationHelper.accountHelper.addNotarySignatory(keypair)

        integrationHelper.runEthNotary(notaryConfig)

        // send ETH
        integrationHelper.sendERC20Token(tokenAddress, amount, relayWallet)
        integrationHelper.waitOneIrohaBlock()

        Assertions.assertEquals(
            BigDecimal(amount, tokenInfo.precision.toInt()).add(BigDecimal(initialAmount)),
            BigDecimal(integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId))
        )
    }

}
