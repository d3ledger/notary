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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import pregeneration.btc.BtcPreGenInitialization
import provider.NotaryPeerListProviderImpl
import provider.TriggerProvider
import provider.btc.BtcPublicKeyProvider
import provider.btc.BtcSessionProvider
import provider.btc.network.BtcRegTestConfigProvider
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil
import util.getRandomString
import wallet.WalletFile
import java.io.File

private const val WAIT_PREGEN_INIT_MILLIS = 10_000L
private const val WAIT_PREGEN_PROCESS_MILLIS = 15_000L

@Disabled
class BtcPreGenIntegrationTest {

    private val integrationHelper = IntegrationHelperUtil()

    private val btcPreGenConfig =
        integrationHelper.configHelper.createBtcPreGenConfig()

    private val triggerProvider = TriggerProvider(
        btcPreGenConfig.iroha,
        integrationHelper.testCredential,
        btcPreGenConfig.pubKeyTriggerAccount
    )
    private val btcKeyGenSessionProvider = BtcSessionProvider(
        btcPreGenConfig.iroha,
        integrationHelper.accountHelper.registrationAccount
    )

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
            BtcPreGenInitialization(
                registrationCredential,
                btcPreGenConfig,
                btcPublicKeyProvider(),
                irohaChainListener()
            ).init()
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

    private val sessionConsumer =
        IrohaConsumerImpl(registrationCredential, pregeneration.btc.config.btcPreGenConfig.iroha)

    private val multiSigConsumer = IrohaConsumerImpl(
        IrohaCredential(btcPreGenConfig.mstRegistrationAccount.accountId, mstRegistrationKeyPair),
        pregeneration.btc.config.btcPreGenConfig.iroha
    )

    fun btcPublicKeyProvider(): BtcPublicKeyProvider {
        val file = File(btcPreGenConfig.btcWalletFilePath)
        val wallet = Wallet.loadFromFile(file)
        val walletFile = WalletFile(wallet, file)
        val notaryPeerListProvider = NotaryPeerListProviderImpl(
            btcPreGenConfig.iroha,
            registrationCredential,
            btcPreGenConfig.notaryListStorageAccount,
            btcPreGenConfig.notaryListSetterAccount
        )
        return BtcPublicKeyProvider(
            walletFile,
            notaryPeerListProvider,
            btcPreGenConfig.notaryAccount,
            multiSigConsumer,
            sessionConsumer,
            BtcRegTestConfigProvider()
        )
    }

    fun irohaChainListener() = IrohaChainListener(
        btcPreGenConfig.iroha.hostname,
        btcPreGenConfig.iroha.port,
        registrationCredential
    )

    /**
     * Logger
     */
    companion object : KLogging()
}
