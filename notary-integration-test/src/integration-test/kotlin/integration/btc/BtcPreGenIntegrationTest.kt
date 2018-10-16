package integration.btc

import integration.helper.IntegrationHelperUtil
import kotlinx.coroutines.experimental.launch
import model.IrohaCredential
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
import pregeneration.btc.BtcPreGenInitialization
import provider.NotaryPeerListProviderImpl
import provider.TriggerProvider
import provider.btc.BtcPublicKeyProvider
import provider.btc.BtcSessionProvider
import provider.btc.network.BtcRegTestConfigProvider
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import util.getRandomString
import wallet.WalletFile
import java.io.File

private const val WAIT_PREGEN_INIT_MILLIS = 10_000L
private const val WAIT_PREGEN_PROCESS_MILLIS = 15_000L

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BtcPreGenIntegrationTest {

    private val integrationHelper = IntegrationHelperUtil()

    private val btcPreGenConfig =
        integrationHelper.configHelper.createBtcPreGenConfig()

    private val irohaNetwork = IrohaNetworkImpl(btcPreGenConfig.iroha.hostname, btcPreGenConfig.iroha.port)

    private val triggerProvider = TriggerProvider(
        integrationHelper.testCredential,
        btcPreGenConfig.pubKeyTriggerAccount,
        irohaNetwork
    )
    private val btcKeyGenSessionProvider = BtcSessionProvider(
        integrationHelper.accountHelper.registrationAccount,
        irohaNetwork
    )

    @AfterAll
    fun dropDown() {
        integrationHelper.close()
        irohaNetwork.close()
    }

    /**
     * Test US-001 btc addresses pregeneration
     * Note: Iroha must be deployed to pass the test.
     * @given session account is created
     * @when special trigger account is triggered
     * @then new multisig btc address is created
     */
    @Test
    fun testGenerateKey() {
        integrationHelper.addNotary("test_notary", "test_notary_address")
        launch {
            IrohaChainListener(
                btcPreGenConfig.iroha.hostname,
                btcPreGenConfig.iroha.port,
                registrationCredential
            ).use { irohaListener ->
                BtcPreGenInitialization(
                    registrationCredential,
                    irohaNetwork,
                    btcPreGenConfig,
                    btcPublicKeyProvider(),
                    irohaListener
                ).init()
            }
        }
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
                btcPreGenConfig.registrationAccount.accountId
            )
        val pubKey = sessionDetails.values.iterator().next()
        assertNotNull(pubKey)
        val wallet = Wallet.loadFromFile(File(btcPreGenConfig.btcWalletFilePath))
        assertNotNull(wallet.issuedReceiveKeys.find { ecKey -> ecKey.publicKeyAsHex == pubKey })
        val notaryAccountDetails =
            integrationHelper.getAccountDetails(
                btcPreGenConfig.notaryAccount,
                btcPreGenConfig.mstRegistrationAccount.accountId
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

    private val registrationKeyPair =
        ModelUtil.loadKeypair(
            btcPreGenConfig.registrationAccount.pubkeyPath,
            btcPreGenConfig.registrationAccount.privkeyPath
        ).fold({ keypair ->
            keypair
        }, { ex -> throw ex })

    private val registrationCredential =
        IrohaCredential(btcPreGenConfig.registrationAccount.accountId, registrationKeyPair)

    private val mstRegistrationKeyPair =
        ModelUtil.loadKeypair(
            btcPreGenConfig.mstRegistrationAccount.pubkeyPath,
            btcPreGenConfig.mstRegistrationAccount.privkeyPath
        ).fold({ keypair ->
            keypair
        }, { ex -> throw ex })

    private val mstRegistrationCredential =
        IrohaCredential(btcPreGenConfig.mstRegistrationAccount.accountId, mstRegistrationKeyPair)


    fun btcPublicKeyProvider(): BtcPublicKeyProvider {
        val file = File(btcPreGenConfig.btcWalletFilePath)
        val wallet = Wallet.loadFromFile(file)
        val walletFile = WalletFile(wallet, file)
        val notaryPeerListProvider = NotaryPeerListProviderImpl(
            registrationCredential,
            btcPreGenConfig.notaryListStorageAccount,
            btcPreGenConfig.notaryListSetterAccount,
            irohaNetwork
        )
        return BtcPublicKeyProvider(
            walletFile,
            irohaNetwork,
            notaryPeerListProvider,
            registrationCredential,
            mstRegistrationCredential,
            btcPreGenConfig.notaryAccount,
            BtcRegTestConfigProvider()
        )
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
