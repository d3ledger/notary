package integration.btc

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
import org.bitcoinj.core.Address
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Utils
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.wallet.Wallet
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import java.io.File
import java.util.*

const val WAIT_PREGEN_PROCESS_MILLIS = 20_000L

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BtcAddressGenerationIntegrationTest {

    private val nodeId = UUID.randomUUID()

    private val integrationHelper = BtcIntegrationHelperUtil()

    private val environment =
        BtcAddressGenerationTestEnvironment(integrationHelper)

    private fun createMsAddress(notaryKeys: Collection<String>): Script {
        val keys = ArrayList<ECKey>()
        notaryKeys.forEach { key ->
            val ecKey = ECKey.fromPublicOnly(Utils.parseAsHexOrBase58(key))
            keys.add(ecKey)
        }
        return ScriptBuilder.createP2SHOutputScript(1, keys)
    }

    private fun getBase58Address(script: Script): String {
        return script.getToAddress(RegTestParams.get()).toBase58()
    }

    private fun getAddress(script: Script): Address {
        return script.getToAddress(RegTestParams.get())
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
        // Wait for initial address generation
        Thread.sleep(WAIT_PREGEN_PROCESS_MILLIS * environment.btcGenerationConfig.threshold)
        environment.checkIfAddressesWereGeneratedAtInitialPhase()
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given "free" session account is created
     * @when special generation account is triggered
     * @then new free multisig btc address is created
     */
    @Test
    fun testGenerateFreeAddress() {
        val sessionAccountName = BtcAddressType.FREE.createSessionAccountName()
        environment.btcKeyGenSessionProvider.createPubKeyCreationSession(
            sessionAccountName,
            nodeId.toString()
        ).fold({ logger.info { "session $sessionAccountName was created" } },
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
        val pubKey = notaryKeys.first()
        assertNotNull(pubKey)
        val wallet = Wallet.loadFromFile(File(environment.btcGenerationConfig.btcKeysWalletPath))
        assertTrue(wallet.issuedReceiveKeys.any { ecKey -> ecKey.publicKeyAsHex == pubKey })
        val notaryAccountDetails =
            integrationHelper.getAccountDetails(
                environment.btcGenerationConfig.notaryAccount,
                environment.btcGenerationConfig.mstRegistrationAccount.accountId
            )
        val expectedMsAddress = createMsAddress(notaryKeys)
        assertTrue(wallet.isAddressWatched(getAddress(expectedMsAddress)))
        val generatedAddress = AddressInfo.fromJson(notaryAccountDetails[getBase58Address(expectedMsAddress)]!!)!!
        assertNull(generatedAddress.irohaClient)
        assertEquals(notaryKeys, generatedAddress.notaryKeys.toList())
        assertEquals(nodeId.toString(), generatedAddress.nodeId)
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
        environment.btcKeyGenSessionProvider.createPubKeyCreationSession(
            sessionAccountName,
            nodeId.toString()
        ).fold({ logger.info { "session $sessionAccountName was created" } },
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
        val pubKey = notaryKeys.first()
        assertNotNull(pubKey)
        val wallet = Wallet.loadFromFile(File(environment.btcGenerationConfig.btcKeysWalletPath))
        assertTrue(wallet.issuedReceiveKeys.any { ecKey -> ecKey.publicKeyAsHex == pubKey })
        val changeAddressStorageAccountDetails =
            integrationHelper.getAccountDetails(
                environment.btcGenerationConfig.changeAddressesStorageAccount,
                environment.btcGenerationConfig.mstRegistrationAccount.accountId
            )
        val expectedMsAddress = createMsAddress(notaryKeys)
        val generatedAddress =
            AddressInfo.fromJson(changeAddressStorageAccountDetails[getBase58Address(expectedMsAddress)]!!)!!
        assertNull(generatedAddress.irohaClient)
        assertEquals(notaryKeys, generatedAddress.notaryKeys.toList())
        assertEquals(nodeId.toString(), generatedAddress.nodeId)
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
