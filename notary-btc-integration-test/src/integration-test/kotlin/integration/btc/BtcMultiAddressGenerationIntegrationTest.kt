package integration.btc

import com.d3.btc.helper.address.getSignThreshold
import com.d3.btc.model.AddressInfo
import com.d3.btc.model.BtcAddressType
import com.d3.btc.provider.generation.ADDRESS_GENERATION_TIME_KEY
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
import org.junit.jupiter.api.*
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BtcMultiAddressGenerationIntegrationTest {

    private val peers = 3
    private val integrationHelper = BtcIntegrationHelperUtil(peers)
    private val environments = ArrayList<BtcAddressGenerationTestEnvironment>()

    @AfterAll
    fun dropDown() {
        environments.forEach { environment ->
            environment.close()
        }
    }

    init {
        var peerCount = 0
        integrationHelper.accountHelper.mstRegistrationAccounts.forEach { mstRegistrationAccount ->
            integrationHelper.addNotary("test_notary_${peerCount++}", "test_notary_address")
            val environment = BtcAddressGenerationTestEnvironment(
                integrationHelper,
                btcGenerationConfig = integrationHelper.configHelper.createBtcAddressGenerationConfig(0),
                mstRegistrationCredential = mstRegistrationAccount
            )
            environments.add(environment)
            GlobalScope.launch {
                environment.btcAddressGenerationInitialization.init().failure { ex -> throw ex }
            }
        }
        //Wait services to init
        Thread.sleep(WAIT_PREGEN_PROCESS_MILLIS)
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given 3 address generation services working in a multisig mode and one "free" session account
     * @when special generation account is triggered
     * @then new free multisig btc address is created
     */
    @Test
    fun testGenerateFreeAddress() {
        val environment = environments.first()
        val sessionAccountName = BtcAddressType.FREE.createSessionAccountName()
        environment.btcKeyGenSessionProvider.createPubKeyCreationSession(sessionAccountName)
            .fold({ BtcAddressGenerationIntegrationTest.logger.info { "session $sessionAccountName was created" } },
                { ex -> fail("cannot create session", ex) })
        environment.triggerProvider.trigger(sessionAccountName)
        Thread.sleep(WAIT_PREGEN_PROCESS_MILLIS * peers)
        val sessionDetails =
            integrationHelper.getAccountDetails(
                "$sessionAccountName@btcSession",
                environment.btcGenerationConfig.registrationAccount.accountId
            )
        val notaryKeys = sessionDetails.entries.filter { entry -> entry.key != ADDRESS_GENERATION_TIME_KEY }
            .map { entry -> entry.value }
        val pubKey = notaryKeys.first()
        Assertions.assertNotNull(pubKey)
        val wallet = Wallet.loadFromFile(File(environment.btcGenerationConfig.btcWalletFilePath))
        Assertions.assertTrue(wallet.issuedReceiveKeys.any { ecKey -> ecKey.publicKeyAsHex == pubKey })
        val notaryAccountDetails =
            integrationHelper.getAccountDetails(
                environment.btcGenerationConfig.notaryAccount,
                environment.btcGenerationConfig.mstRegistrationAccount.accountId
            )
        val expectedMsAddress = createMsAddress(notaryKeys)
        val generatedAddress = AddressInfo.fromJson(notaryAccountDetails[expectedMsAddress]!!)!!
        Assertions.assertEquals(BtcAddressType.FREE.title, generatedAddress.irohaClient)
        Assertions.assertEquals(notaryKeys, generatedAddress.notaryKeys.toList())
    }

    private fun createMsAddress(notaryKeys: Collection<String>): String {
        val keys = ArrayList<ECKey>()
        notaryKeys.forEach { key ->
            val ecKey = ECKey.fromPublicOnly(Utils.parseAsHexOrBase58(key))
            keys.add(ecKey)
        }
        val script = ScriptBuilder.createP2SHOutputScript(getSignThreshold(peers), keys)
        return script.getToAddress(RegTestParams.get()).toBase58()
    }

    /**
     * Logger
     */
    companion object : KLogging()

}
