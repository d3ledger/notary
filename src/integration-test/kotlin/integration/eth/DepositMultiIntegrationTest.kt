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
 * Integration tests with multiple notaries for deposit case.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DepositmultiIntegrationTest {
    /** Utility functions for integration tests */
    private val integrationHelper = IntegrationHelperUtil()

    /** Ethereum assetId in Iroha */
    private val etherAssetId = "ether#ethereum"

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

    init {
        // run notary
        integrationHelper.runEthNotary()

        // create 2nd notray config
        val irohaConfig =
            integrationHelper.configHelper.createIrohaConfig(pubkeyPath = pubkeyPath, privkeyPath = privkeyPath)
        val notaryConfig = integrationHelper.configHelper.createEthNotaryConfig(irohaConfig)

        val keypair = ModelUtil.loadKeypair(pubkeyPath, privkeyPath).get()

        integrationHelper.accountHelper.addNotarySignatory(keypair)

        // run 2nd instance of notary
        integrationHelper.runEthNotary(notaryConfig)
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

        // send ETH
        integrationHelper.sendERC20Token(tokenAddress, amount, relayWallet)
        integrationHelper.waitOneIrohaBlock()

        Assertions.assertEquals(
            BigDecimal(amount, tokenInfo.precision.toInt()).add(BigDecimal(initialAmount)),
            BigDecimal(integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId))
        )
    }

}
