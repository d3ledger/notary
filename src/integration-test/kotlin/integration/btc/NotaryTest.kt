package integration.btc

import integration.helper.IntegrationHelperUtil
import kotlinx.coroutines.experimental.async
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger


class NotaryTest {
    init {
        async {
            notary.btc.main(emptyArray())
        }
        Thread.sleep(15_000)
    }

    private val integrationHelper = IntegrationHelperUtil()
    //TODO don't forget to change. See BtcNotaryInitialization
    private val testClient = "test@notary"
    private val btcAddress = "mjQJGeVpe5UuraPkdXogVy6vT2dixAxCyR"

    private val btcAsset = "btc#bitcoin"

    /**
     * Test US-001 Deposit of
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given Iroha networks and bitcoind  are running
     * @when 1 btc was sent to watched address using bitcoin-cli
     * @then balance of client with related address is increased by 1 btc(or 100.000.000 sat)
     */
    @Test
    fun testDeposit() {
        val initialBalance = integrationHelper.getIrohaBalance(btcAsset, testClient)
        val btcAmount = 1
        integrationHelper.sendBtc(btcAddress, btcAmount)
        Thread.sleep(20_000)
        val newBalance = integrationHelper.getIrohaBalance(btcAsset, testClient)
        assertEquals(
            initialBalance.add(BigInteger.valueOf(btcToSat(btcAmount))),
            newBalance
        )
    }

    private fun btcToSat(btc: Int): Long {
        return btc * 100_000_000L
    }
}
