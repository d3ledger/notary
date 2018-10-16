package integration.btc

import com.github.kittinunf.result.failure
import integration.helper.IntegrationHelperUtil
import model.IrohaCredential
import notary.btc.BtcNotaryInitialization
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import provider.btc.BtcRegisteredAddressesProvider
import provider.btc.network.BtcRegTestConfigProvider
import sidechain.iroha.CLIENT_DOMAIN
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import util.getRandomString
import java.math.BigDecimal

private val integrationHelper = IntegrationHelperUtil()
private val btcAsset = "btc#bitcoin"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BtcNotaryIntegrationTest {

    init {
        integrationHelper.generateBtcBlocks()
        integrationHelper.addNotary("test_notary", "test_notary_address")
    }

    private val notaryConfig = integrationHelper.configHelper.createBtcNotaryConfig()
    private val irohaCredential = IrohaCredential(
        notaryConfig.notaryCredential.accountId,
        ModelUtil.loadKeypair(notaryConfig.notaryCredential.pubkeyPath, notaryConfig.notaryCredential.privkeyPath).get()
    )
    private val irohaNetwork = IrohaNetworkImpl(notaryConfig.iroha.hostname, notaryConfig.iroha.port)

    private val btcRegisteredAddressesProvider = BtcRegisteredAddressesProvider(
        irohaCredential,
        irohaNetwork,
        notaryConfig.registrationAccount,
        notaryConfig.notaryCredential.accountId
    )

    private val btcNetworkConfigProvider = BtcRegTestConfigProvider()

    private val btcNotaryInitialization =
        BtcNotaryInitialization(
            notaryConfig,
            irohaCredential,
            irohaNetwork,
            btcRegisteredAddressesProvider,
            btcNetworkConfigProvider
        )

    @AfterAll
    fun dropDown() {
        integrationHelper.close()
        irohaNetwork.close()
    }

    /**
     * Test US-001 Deposit
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given new registered account
     * @when 1 btc was sent to new account
     * @then balance of new account is increased by 1 btc(or 100.000.000 sat)
     */
    @Test
    fun testDeposit() {
        btcNotaryInitialization.init().failure { ex -> fail("Cannot run BTC notary", ex) }
        val randomName = String.getRandomString(9)
        val testClient = "$randomName@$CLIENT_DOMAIN"
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
