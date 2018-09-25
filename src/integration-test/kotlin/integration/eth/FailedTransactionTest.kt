package integration.eth

import integration.helper.IntegrationHelperUtil
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigInteger
import util.getRandomString
import java.math.BigDecimal
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
        integrationHelper.registerClientWithoutRelay(clientAccount)
        integrationHelper.sendEth(BigInteger.valueOf(1), failerAddress)
        Thread.sleep(15_000)
        assertEquals(BigInteger.ZERO, integrationHelper.getEthBalance(failerAddress))
        val irohaBalance = integrationHelper.getIrohaAccountBalance("$clientAccount@notary", "ether#ethereum")
        assertEquals(BigDecimal.ZERO, BigDecimal(irohaBalance))
    }
}
