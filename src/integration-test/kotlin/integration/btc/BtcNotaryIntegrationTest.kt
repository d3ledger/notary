package integration.btc

import com.github.kittinunf.result.failure
import integration.helper.IntegrationHelperUtil
import notary.btc.BtcNotaryInitialization
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import provider.btc.BtcRegisteredAddressesProvider
import sidechain.iroha.util.ModelUtil
import util.getRandomString
import java.math.BigDecimal

private val integrationHelper = IntegrationHelperUtil()
private val btcAsset = "btc#bitcoin"

class BtcNotaryIntegrationTest {

    private val notaryConfig = integrationHelper.configHelper.createBtcNotaryConfig()

    private val btcRegisteredAddressesProvider by lazy {
        ModelUtil.loadKeypair(notaryConfig.iroha.pubkeyPath, notaryConfig.iroha.privkeyPath).fold({ keypair ->
            BtcRegisteredAddressesProvider(
                notaryConfig.iroha,
                keypair,
                notaryConfig.registrationAccount,
                notaryConfig.iroha.creator
            )
        }, { ex -> throw ex })
    }

    private val btcNotaryInitialization = BtcNotaryInitialization(notaryConfig, btcRegisteredAddressesProvider)


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
        btcNotaryInitialization.init().failure { ex -> fail("Cannot run BTC notary", ex) }
        val randomName = String.getRandomString(9)
        val testClient = "$randomName@notary"
        val btcAddress = integrationHelper.registerBtcAddress(randomName)
        val initialBalance = integrationHelper.getIrohaAccountBalance(
            testClient,
            btcAsset
        )
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
