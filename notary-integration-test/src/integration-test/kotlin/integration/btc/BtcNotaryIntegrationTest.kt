package integration.btc

import com.github.kittinunf.result.failure
import integration.helper.IntegrationHelperUtil
import model.IrohaCredential
import notary.btc.BtcNotaryInitialization
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import provider.btc.BtcRegisteredAddressesProvider
import provider.btc.network.BtcRegTestConfigProvider
import sidechain.iroha.util.ModelUtil
import util.getRandomString
import java.math.BigDecimal

private val integrationHelper = IntegrationHelperUtil()
private val btcAsset = "btc#bitcoin"

class BtcNotaryIntegrationTest {

    private val notaryConfig = integrationHelper.configHelper.createBtcNotaryConfig()

    private val btcRegisteredAddressesProvider by lazy {
        ModelUtil.loadKeypair(notaryConfig.notaryCredential.pubkeyPath, notaryConfig.notaryCredential.privkeyPath)
            .fold({ keypair ->
                BtcRegisteredAddressesProvider(
                    notaryConfig.iroha,
                    IrohaCredential(notaryConfig.notaryCredential.accountId, keypair),
                    notaryConfig.registrationAccount,
                    notaryConfig.notaryCredential.accountId
                )
            }, { ex -> throw ex })
    }

    private val btcNetworkConfigProvider = BtcRegTestConfigProvider()

    private val btcNotaryInitialization =
        BtcNotaryInitialization(notaryConfig, btcRegisteredAddressesProvider, btcNetworkConfigProvider)


    /**
     * Test US-001 Deposit
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given new registered account
     * @when 1 btc was sent to new account
     * @then balance of new account is increased by 1 btc(or 100.000.000 sat)
     */
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
        integrationHelper.sendBtc(btcAddress, btcAmount)
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
