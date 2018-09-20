package integration.btc

import integration.helper.IntegrationHelperUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import util.getRandomString
import java.math.BigDecimal


class BtcNotaryIntegrationTest {

    private val integrationHelper = IntegrationHelperUtil()
    private val btcAsset = "btc#bitcoin"

    /**
     * Test US-001 Deposit
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given new registered account
     * @when 1 btc was sent to new account
     * @then balance of new account is increased by 1 btc(or 100.000.000 sat)
     */
    @Disabled
    @Test
    fun testDeposit() {
        integrationHelper.generateBtcBlocks()
        integrationHelper.addNotary("test_notary", "test_notary_address")
        notary.btc.executeNotary(integrationHelper.configHelper.createBtcNotaryConfig())
        val randomName = String.getRandomString(9)
        val testClient = "$randomName@notary"
        val btcAddress = integrationHelper.registerBtcAddress(randomName)
        val initialBalance = integrationHelper.getIrohaAccountBalance(testClient, btcAsset)
        val btcAmount = 1
        if (!integrationHelper.sendBtc(btcAddress, btcAmount)) {
            fail { "failed to send btc" }
        }
        Thread.sleep(30_000)
        val newBalance = integrationHelper.getIrohaAccountBalance(testClient, btcAsset)
        assertEquals(
            BigDecimal(initialBalance).add(BigDecimal.valueOf(btcToSat(btcAmount))).toString(),
            newBalance
        )
    }

    private fun btcToSat(btc: Int): Long {
        return btc * 100_000_000L
    }
}
