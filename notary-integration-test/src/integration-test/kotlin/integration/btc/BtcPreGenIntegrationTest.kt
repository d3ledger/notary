package integration.btc

import integration.helper.IntegrationHelperUtil
import kotlinx.coroutines.experimental.launch
import mu.KLogging
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Utils
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.wallet.Wallet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import provider.TriggerProvider
import provider.btc.BtcSessionProvider
import registration.btc.pregen.executePreGeneration
import util.getRandomString
import java.io.File

private const val WAIT_PREGEN_INIT_MILLIS = 10_000L
private const val WAIT_PREGEN_PROCESS_MILLIS = 15_000L

class BtcPreGenIntegrationTest {

    private val integrationHelper = IntegrationHelperUtil()

    private val btcPkPreGenConfig =
        integrationHelper.configHelper.createBtcPreGenConfig()

    private val triggerProvider = TriggerProvider(
        btcPkPreGenConfig.iroha,
        integrationHelper.testCredential,
        btcPkPreGenConfig.pubKeyTriggerAccount
    )
    private val btcKeyGenSessionProvider = BtcSessionProvider(
        btcPkPreGenConfig.iroha,
        integrationHelper.accountHelper.registrationAccount
    )

    /**
     * Test US-001 btc addresses pregeneration
     * Note: Iroha must be deployed to pass the test.
     * @given session account is created
     * @when special trigger account is triggered
     * @then new multisig btc address is created
     */
    @Disabled
    @Test
    fun testGenerateKey() {
        integrationHelper.addNotary("test_notary", "test_notary_address")
        launch { executePreGeneration(btcPkPreGenConfig) }
        Thread.sleep(WAIT_PREGEN_INIT_MILLIS)
        val sessionAccountName = String.getRandomString(9)
        btcKeyGenSessionProvider.createPubKeyCreationSession(sessionAccountName)
            .fold({ logger.info { "session $sessionAccountName was created" } },
                { ex -> fail("cannot create session", ex) })
        triggerProvider.trigger(sessionAccountName)
        Thread.sleep(WAIT_PREGEN_PROCESS_MILLIS)
        val sessionDetails =
            integrationHelper.getAccountDetails(
                "$sessionAccountName@btcSession",
                btcPkPreGenConfig.registrationAccount.accountId
            )
        val pubKey = sessionDetails.values.iterator().next()
        assertNotNull(pubKey)
        val wallet = Wallet.loadFromFile(File(btcPkPreGenConfig.btcWalletFilePath))
        assertNotNull(wallet.issuedReceiveKeys.find { ecKey -> ecKey.publicKeyAsHex == pubKey })
        val notaryAccountDetails =
            integrationHelper.getAccountDetails(
                btcPkPreGenConfig.notaryAccount,
                btcPkPreGenConfig.mstRegistrationAccount.accountId
            )
        val expectedMsAddress = createMstAddress(sessionDetails.values)
        assertEquals("free", notaryAccountDetails.get(expectedMsAddress))
    }

    private fun createMstAddress(notaryKeys: Collection<String>): String {
        val keys = ArrayList<ECKey>()
        notaryKeys.forEach { key ->
            val ecKey = ECKey.fromPublicOnly(Utils.parseAsHexOrBase58(key))
            keys.add(ecKey)
        }
        val script = ScriptBuilder.createP2SHOutputScript(1, keys)
        return script.getToAddress(RegTestParams.get()).toBase58()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
