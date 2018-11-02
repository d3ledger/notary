package integration.eth

import integration.helper.IntegrationHelperUtil
import jdk.nashorn.internal.ir.annotations.Ignore
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import provider.eth.ETH_PRECISION
import sidechain.iroha.CLIENT_DOMAIN
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
        integrationHelper.lockEthMasterSmartcontract()
    }

    /** Iroha client account */
    private val clientIrohaAccount = String.getRandomString(9)
    private val clientIrohaAccountId = "$clientIrohaAccount@$CLIENT_DOMAIN"

    /** Ethereum address to transfer to */
    private val relayWallet = registerRelay()

    private fun registerRelay(): String {
        integrationHelper.deployRelays(1)
        // TODO: D3-417 Web3j cannot pass an empty list of addresses to the smart contract.
        return integrationHelper.registerClient(clientIrohaAccount, listOf())
    }

    @AfterAll
    fun dropDown() {
        integrationHelper.close()
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
     * Test US-003 Deposit of ETH
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running, smart contract with balance
     * @when smart-contract transfers 1234000000000 Wei to "relayWallet"
     * @then Associated Iroha account balance is increased on 1234000000000 Wei
     */
    @Test
    @Ignore
    fun internalTransactionDeposit() {
        val initialAmount = integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, etherAssetId)
        val amount = BigInteger.valueOf(1_234_000_000_000)
        // send ETH
        integrationHelper.sendEth(amount, integrationHelper.internalTxProducer.contractAddress)
        integrationHelper.internalTxProducer.sendFunds(relayWallet).send()
        integrationHelper.waitOneIrohaBlock()

        Assertions.assertEquals(
            BigDecimal(amount, ETH_PRECISION.toInt()).add(BigDecimal(initialAmount)),
            BigDecimal(integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, etherAssetId))
        )
    }

}
