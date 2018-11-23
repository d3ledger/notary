package integration.btc

import generation.btc.BtcAddressGenerationInitialization
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
import provider.NotaryPeerListProviderImpl
import provider.TriggerProvider
import provider.btc.BtcPublicKeyProvider
import provider.btc.BtcSessionProvider
import provider.btc.address.AddressInfo
import provider.btc.address.BtcAddressType
import provider.btc.network.BtcRegTestConfigProvider
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil
import wallet.WalletFile
import java.io.File

private const val WAIT_PREGEN_INIT_MILLIS = 10_000L
private const val WAIT_PREGEN_PROCESS_MILLIS = 15_000L

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BtcAddressGenerationIntegrationTest {

    private val integrationHelper = IntegrationHelperUtil()

    private val btcGenerationConfig =
        integrationHelper.configHelper.createBtcAddressGenerationConfig()

    private val triggerProvider = TriggerProvider(
        integrationHelper.testCredential,
        integrationHelper.irohaNetwork,
        btcGenerationConfig.pubKeyTriggerAccount
    )
    private val btcKeyGenSessionProvider = BtcSessionProvider(
        integrationHelper.accountHelper.registrationAccount,
        integrationHelper.irohaNetwork
    )

    private fun createMsAddress(notaryKeys: Collection<String>): String {
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
            btcGenerationConfig.registrationAccount.pubkeyPath,
            btcGenerationConfig.registrationAccount.privkeyPath
        ).fold({ keypair ->
            keypair
        }, { ex -> throw ex })

    private val registrationCredential =
        IrohaCredential(btcGenerationConfig.registrationAccount.accountId, registrationKeyPair)

    private val mstRegistrationKeyPair =
        ModelUtil.loadKeypair(
            btcGenerationConfig.mstRegistrationAccount.pubkeyPath,
            btcGenerationConfig.mstRegistrationAccount.privkeyPath
        ).fold({ keypair ->
            keypair
        }, { ex -> throw ex })

    private val sessionConsumer =
        IrohaConsumerImpl(registrationCredential, integrationHelper.irohaNetwork)

    private val multiSigConsumer = IrohaConsumerImpl(
        IrohaCredential(btcGenerationConfig.mstRegistrationAccount.accountId, mstRegistrationKeyPair),
        integrationHelper.irohaNetwork
    )

    @AfterAll
    fun dropDown() {
        integrationHelper.close()
    }

    init {
        integrationHelper.addNotary("test_notary", "test_notary_address")
        launch {
            IrohaChainListener(
                btcGenerationConfig.iroha.hostname,
                btcGenerationConfig.iroha.port,
                registrationCredential
            ).use { irohaListener ->
                BtcAddressGenerationInitialization(
                    registrationCredential,
                    integrationHelper.irohaNetwork,
                    btcGenerationConfig,
                    btcPublicKeyProvider(),
                    irohaListener
                ).init()
            }
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
        btcKeyGenSessionProvider.createPubKeyCreationSession(sessionAccountName)
            .fold({ logger.info { "session $sessionAccountName was created" } },
                { ex -> fail("cannot create session", ex) })
        triggerProvider.trigger(sessionAccountName)
        Thread.sleep(WAIT_PREGEN_PROCESS_MILLIS)
        val sessionDetails =
            integrationHelper.getAccountDetails(
                "$sessionAccountName@btcSession",
                btcGenerationConfig.registrationAccount.accountId
            )
        val pubKey = sessionDetails.values.iterator().next()
        assertNotNull(pubKey)
        val wallet = Wallet.loadFromFile(File(btcGenerationConfig.btcWalletFilePath))
        assertNotNull(wallet.issuedReceiveKeys.find { ecKey -> ecKey.publicKeyAsHex == pubKey })
        val notaryAccountDetails =
            integrationHelper.getAccountDetails(
                btcGenerationConfig.notaryAccount,
                btcGenerationConfig.mstRegistrationAccount.accountId
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
        btcKeyGenSessionProvider.createPubKeyCreationSession(sessionAccountName)
            .fold({ logger.info { "session $sessionAccountName was created" } },
                { ex -> fail("cannot create session", ex) })
        triggerProvider.trigger(sessionAccountName)
        Thread.sleep(WAIT_PREGEN_PROCESS_MILLIS)
        val sessionDetails =
            integrationHelper.getAccountDetails(
                "$sessionAccountName@btcSession",
                btcGenerationConfig.registrationAccount.accountId
            )
        val pubKey = sessionDetails.values.iterator().next()
        assertNotNull(pubKey)
        val wallet = Wallet.loadFromFile(File(btcGenerationConfig.btcWalletFilePath))
        assertNotNull(wallet.issuedReceiveKeys.find { ecKey -> ecKey.publicKeyAsHex == pubKey })
        val changeAddressStorageAccountDetails =
            integrationHelper.getAccountDetails(
                btcGenerationConfig.changeAddressesStorageAccount,
                btcGenerationConfig.mstRegistrationAccount.accountId
            )
        val expectedMsAddress = createMsAddress(sessionDetails.values)
        val generatedAddress = AddressInfo.fromJson(changeAddressStorageAccountDetails[expectedMsAddress]!!)!!
        assertEquals(BtcAddressType.CHANGE.title, generatedAddress.irohaClient)
        assertEquals(sessionDetails.values.toList(), generatedAddress.notaryKeys.toList())
    }

    private fun btcPublicKeyProvider(): BtcPublicKeyProvider {
        val file = File(btcGenerationConfig.btcWalletFilePath)
        val wallet = Wallet.loadFromFile(file)
        val walletFile = WalletFile(wallet, file)
        val notaryPeerListProvider = NotaryPeerListProviderImpl(
            registrationCredential,
            integrationHelper.irohaNetwork,
            btcGenerationConfig.notaryListStorageAccount,
            btcGenerationConfig.notaryListSetterAccount
        )
        return BtcPublicKeyProvider(
            walletFile,
            notaryPeerListProvider,
            btcGenerationConfig.notaryAccount,
            btcGenerationConfig.changeAddressesStorageAccount,
            multiSigConsumer,
            sessionConsumer,
            BtcRegTestConfigProvider()
        )
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
