package integration.btc

import com.d3.btc.helper.address.getSignThreshold
import com.d3.btc.model.AddressInfo
import com.d3.btc.model.BtcAddressType
import com.d3.btc.provider.generation.ADDRESS_GENERATION_NODE_ID_KEY
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
import org.junit.Assert.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import java.io.File
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BtcMultiAddressGenerationIntegrationTest {

    private val nodeId = UUID.randomUUID()
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
        environment.btcKeyGenSessionProvider.createPubKeyCreationSession(
            sessionAccountName,
            nodeId.toString()
        ).fold({ BtcAddressGenerationIntegrationTest.logger.info { "session $sessionAccountName was created" } },
            { ex -> fail("cannot create session", ex) })
        environment.triggerProvider.trigger(sessionAccountName)
        Thread.sleep(WAIT_PREGEN_PROCESS_MILLIS)
        val sessionDetails =
            integrationHelper.getAccountDetails(
                "$sessionAccountName@btcSession",
                environment.btcGenerationConfig.registrationAccount.accountId
            )
        val notaryKeys =
            sessionDetails.entries.filter { entry ->
                entry.key != ADDRESS_GENERATION_TIME_KEY
                        && entry.key != ADDRESS_GENERATION_NODE_ID_KEY
            }.map { entry -> entry.value }
        val wallet = Wallet.loadFromFile(File(environment.btcGenerationConfig.btcWalletFilePath))
        notaryKeys.forEach { pubKey ->
            assertTrue(wallet.issuedReceiveKeys.any { ecKey -> ecKey.publicKeyAsHex == pubKey })
        }
        val notaryAccountDetails =
            integrationHelper.getAccountDetails(
                environment.btcGenerationConfig.notaryAccount,
                environment.btcGenerationConfig.mstRegistrationAccount.accountId
            )
        val expectedMsAddress = createMsAddress(notaryKeys)
        val generatedAddress = AddressInfo.fromJson(notaryAccountDetails[expectedMsAddress]!!)!!
        assertNull(generatedAddress.irohaClient)
        assertEquals(notaryKeys, generatedAddress.notaryKeys.toList())
        assertEquals(nodeId.toString(), generatedAddress.nodeId)
        assertEquals(
            1,
            integrationHelper.getAccountDetails(
                environment.btcGenerationConfig.notaryAccount,
                environment.btcGenerationConfig.mstRegistrationAccount.accountId
            ).size
        )
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
