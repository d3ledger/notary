package integration.btc

import com.github.kittinunf.result.failure
import integration.btc.environment.BtcAddressGenerationTestEnvironment
import integration.helper.BtcIntegrationHelperUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KLogging
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Utils
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.wallet.Wallet
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import provider.btc.address.AddressInfo
import provider.btc.address.BtcAddressType
import java.io.File

private const val WAIT_PREGEN_INIT_MILLIS = 10_000L
const val WAIT_PREGEN_PROCESS_MILLIS = 15_000L

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BtcAddressGenerationIntegrationTest {

    private val integrationHelper = BtcIntegrationHelperUtil()

    private val environment = BtcAddressGenerationTestEnvironment(integrationHelper)

    private fun createMsAddress(notaryKeys: Collection<String>): String {
        val keys = ArrayList<ECKey>()
        notaryKeys.forEach { key ->
            val ecKey = ECKey.fromPublicOnly(Utils.parseAsHexOrBase58(key))
            keys.add(ecKey)
        }
        val script = ScriptBuilder.createP2SHOutputScript(1, keys)
        return script.getToAddress(RegTestParams.get()).toBase58()
    }

    @AfterAll
    fun dropDown() {
        environment.close()
    }

    init {
        integrationHelper.addNotary("test_notary", "test_notary_address")
        GlobalScope.launch {
            environment.btcAddressGenerationInitialization.init().failure { ex -> throw ex }
        }
        Thread.sleep(WAIT_PREGEN_INIT_MILLIS)
    }

    /**
     * Test US-001 btc addresses generation
     * Note: Iroha must be deployed to pass the test.
     * @given "free" session account is created
     * @when special generation account is triggered
     * @then new free multisig btc address is created
     */
    @Test
    fun testGenerateFreeAddress() {
        val sessionAccountName = BtcAddressType.FREE.createSessionAccountName()
        environment.btcKeyGenSessionProvider.createPubKeyCreationSession(sessionAccountName)
            .fold({ logger.info { "session $sessionAccountName was created" } },
                { ex -> fail("cannot create session", ex) })
        environment.triggerProvider.trigger(sessionAccountName)
        Thread.sleep(WAIT_PREGEN_PROCESS_MILLIS)
        val sessionDetails =
            integrationHelper.getAccountDetails(
                "$sessionAccountName@btcSession",
                environment.btcGenerationConfig.registrationAccount.accountId
            )
        val pubKey = sessionDetails.values.iterator().next()
        assertNotNull(pubKey)
        val wallet = Wallet.loadFromFile(File(environment.btcGenerationConfig.btcWalletFilePath))
        assertNotNull(wallet.issuedReceiveKeys.find { ecKey -> ecKey.publicKeyAsHex == pubKey })
        val notaryAccountDetails =
            integrationHelper.getAccountDetails(
                environment.btcGenerationConfig.notaryAccount,
                environment.btcGenerationConfig.mstRegistrationAccount.accountId
            )
        val expectedMsAddress = createMsAddress(sessionDetails.values)
        val generatedAddress = AddressInfo.fromJson(notaryAccountDetails[expectedMsAddress]!!)!!
        assertEquals(BtcAddressType.FREE.title, generatedAddress.irohaClient)
        assertEquals(sessionDetails.values.toList(), generatedAddress.notaryKeys.toList())
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given "change" session account is created
     * @when special generation account is triggered
     * @then new multisig btc address that stores change is created
     */
    @Test
    fun testGenerateChangeAddress() {
        val sessionAccountName = BtcAddressType.CHANGE.createSessionAccountName()
        environment.btcKeyGenSessionProvider.createPubKeyCreationSession(sessionAccountName)
            .fold({ logger.info { "session $sessionAccountName was created" } },
                { ex -> fail("cannot create session", ex) })
        environment.triggerProvider.trigger(sessionAccountName)
        Thread.sleep(WAIT_PREGEN_PROCESS_MILLIS)
        val sessionDetails =
            integrationHelper.getAccountDetails(
                "$sessionAccountName@btcSession",
                environment.btcGenerationConfig.registrationAccount.accountId
            )
        val pubKey = sessionDetails.values.iterator().next()
        assertNotNull(pubKey)
        val wallet = Wallet.loadFromFile(File(environment.btcGenerationConfig.btcWalletFilePath))
        assertNotNull(wallet.issuedReceiveKeys.find { ecKey -> ecKey.publicKeyAsHex == pubKey })
        val changeAddressStorageAccountDetails =
            integrationHelper.getAccountDetails(
                environment.btcGenerationConfig.changeAddressesStorageAccount,
                environment.btcGenerationConfig.mstRegistrationAccount.accountId
            )
        val expectedMsAddress = createMsAddress(sessionDetails.values)
        val generatedAddress = AddressInfo.fromJson(changeAddressStorageAccountDetails[expectedMsAddress]!!)!!
        assertEquals(BtcAddressType.CHANGE.title, generatedAddress.irohaClient)
        assertEquals(sessionDetails.values.toList(), generatedAddress.notaryKeys.toList())
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
