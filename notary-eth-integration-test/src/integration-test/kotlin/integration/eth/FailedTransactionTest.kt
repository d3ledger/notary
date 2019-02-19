package integration.eth

import integration.helper.EthIntegrationHelperUtil
import integration.helper.IrohaConfigHelper
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.web3j.protocol.exceptions.TransactionException
import sidechain.iroha.CLIENT_DOMAIN
import util.getRandomString
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FailedTransactionTest {
    val integrationHelper = EthIntegrationHelperUtil()

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    init {
        integrationHelper.runEthNotary()
    }

    @AfterAll
    fun dropDown() {
        integrationHelper.close()
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
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val failerAddress = integrationHelper.deployFailer()
            integrationHelper.registerRelayByAddress(failerAddress)
            val clientAccount = String.getRandomString(9)
            integrationHelper.registerClientWithoutRelay(clientAccount, listOf())
            integrationHelper.sendEth(BigInteger.valueOf(1), failerAddress)
            integrationHelper.waitOneEtherBlock()
            assertEquals(BigInteger.ZERO, integrationHelper.getEthBalance(failerAddress))
            val irohaBalance =
                integrationHelper.getIrohaAccountBalance("$clientAccount@$CLIENT_DOMAIN", "ether#ethereum")
            assertEquals(BigDecimal.ZERO, BigDecimal(irohaBalance))
        }
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
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val failerAddress = integrationHelper.deployFailer()
            val anotherFailerAddress = integrationHelper.deployFailer()
            integrationHelper.registerRelayByAddress(failerAddress)
            val clientAccount = String.getRandomString(9)
            integrationHelper.registerClientWithoutRelay(clientAccount, listOf())
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
            val irohaBalance =
                integrationHelper.getIrohaAccountBalance("$clientAccount@$CLIENT_DOMAIN", "$coinName#ethereum")
            assertEquals(BigDecimal.ZERO, BigDecimal(irohaBalance))
        }
    }
}
