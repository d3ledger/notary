package integration.eth

import integration.helper.IntegrationHelperUtil
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.web3j.protocol.exceptions.TransactionException
import util.getRandomString
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FailedTransactionTest {
    val integrationHelper = IntegrationHelperUtil()

    init {
        integrationHelper.runEthNotary()
    }

    /**
     * Reverted Ether transfer transaction test
     * @given Ethereum node, Iroha and notary are running, failer contract is deployed and registered as relay,
     * new user registers in Iroha
     * @when send Ether to relay account
     * @then user account in Iroha has 0 balance of Ether
     */
    @Test
    fun failedEtherTransferTest() {
        val failerAddress = integrationHelper.deployFailer()
        integrationHelper.registerRelayByAddress(failerAddress)
        val clientAccount = String.getRandomString(9)
        integrationHelper.registerClientWithoutRelay(clientAccount, listOf("0x0"))
        integrationHelper.sendEth(BigInteger.valueOf(1), failerAddress)
        integrationHelper.waitOneEtherBlock()
        assertEquals(BigInteger.ZERO, integrationHelper.getEthBalance(failerAddress))
        val irohaBalance = integrationHelper.getIrohaAccountBalance("$clientAccount@notary", "ether#ethereum")
        assertEquals(BigDecimal.ZERO, BigDecimal(irohaBalance))
    }

    /**
     * Reverted Ether transfer transaction test
     * @given Ethereum node, Iroha and notary are running, failer contract is deployed twice,
     * one of them is registered as relay, another one is registered as ERC-20 token and
     * new user registers in Iroha
     * @when send tokens to relay account
     * @then user account in Iroha has 0 balance of ERC-20 token
     */
    @Test
    fun failedTokenTransferTest() {
        val failerAddress = integrationHelper.deployFailer()
        val anotherFailerAddress = integrationHelper.deployFailer()
        integrationHelper.registerRelayByAddress(failerAddress)
        val clientAccount = String.getRandomString(9)
        integrationHelper.registerClientWithoutRelay(clientAccount, listOf("0x0"))
        val coinName = String.getRandomString(9)
        integrationHelper.addERC20Token(anotherFailerAddress, coinName, 0)

        // web3j throws exception in case of contract function call revert
        // so let's catch and ignore it
        try {
            integrationHelper.sendERC20Token(anotherFailerAddress, BigInteger.valueOf(1), failerAddress)
        } catch (e: TransactionException) {
        }

        integrationHelper.waitOneEtherBlock()

        // actually this test passes even without transaction status check
        // it's probably impossible to get some money deposit to iroha
        // because logs are empty for reverted transactions
        // but let's leave it for a rainy day
        val irohaBalance = integrationHelper.getIrohaAccountBalance("$clientAccount@notary", "$coinName#ethereum")
        assertEquals(BigDecimal.ZERO, BigDecimal(irohaBalance))
    }
}
