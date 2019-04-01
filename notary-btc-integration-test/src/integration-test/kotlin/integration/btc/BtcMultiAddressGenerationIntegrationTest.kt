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
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.wallet.Wallet
import org.junit.Assert.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

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
            val walletPostfix = "test-multisig-generation-$peerCount"
            integrationHelper.addNotary("test_notary_${peerCount++}", "test_notary_address")
            val environment = BtcAddressGenerationTestEnvironment(
                integrationHelper,
                btcGenerationConfig = integrationHelper.configHelper.createBtcAddressGenerationConfig(0, walletPostfix),
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
        val expectedMsAddress = com.d3.btc.helper.address.createMsAddress(notaryKeys, RegTestParams.get())
        val wallets = ArrayList<Wallet>()
        environments.forEach { env ->
            wallets.add(Wallet.loadFromFile(File(env.btcGenerationConfig.btcKeysWalletPath)))
        }

        //Check that every wallet has generated address
        wallets.forEach { wallet ->
            assertTrue(wallet.isAddressWatched(expectedMsAddress))
        }

        //Check that every wallet has only one public key that was used in address generation
        val keysSavedInWallets = ArrayList<String>()
        notaryKeys.forEach { pubKey ->
            val walletWithKey = wallets.first { wallet ->
                wallet.issuedReceiveKeys.any { ecKey -> ecKey.publicKeyAsHex == pubKey }
            }
            keysSavedInWallets.add(pubKey)
            wallets.remove(walletWithKey)
        }
        assertEquals(keysSavedInWallets.size, notaryKeys.size)
        val notaryAccountDetails =
            integrationHelper.getAccountDetails(
                environment.btcGenerationConfig.notaryAccount,
                environment.btcGenerationConfig.mstRegistrationAccount.accountId
            )
        val msAddress = expectedMsAddress.toBase58()
        logger.info("Expected address $msAddress")
        val generatedAddress = AddressInfo.fromJson(notaryAccountDetails[msAddress]!!)!!
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

    /**
     * Logger
     */
    companion object : KLogging()

}
