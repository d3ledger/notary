package integration.btc.environment

import generation.btc.BtcAddressGenerationInitialization
import integration.helper.BtcIntegrationHelperUtil
import model.IrohaCredential
import org.bitcoinj.wallet.Wallet
import provider.NotaryPeerListProviderImpl
import provider.TriggerProvider
import provider.btc.generation.BtcPublicKeyProvider
import provider.btc.generation.BtcSessionProvider
import provider.btc.network.BtcRegTestConfigProvider
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil
import wallet.WalletFile
import java.io.Closeable
import java.io.File

/**
 * Bitcoin address generation service testing environment
 */
class BtcAddressGenerationTestEnvironment(private val integrationHelper: BtcIntegrationHelperUtil) : Closeable {


    val btcGenerationConfig =
        integrationHelper.configHelper.createBtcAddressGenerationConfig()

    val triggerProvider = TriggerProvider(
        integrationHelper.testCredential,
        integrationHelper.irohaAPI,
        btcGenerationConfig.pubKeyTriggerAccount
    )
    val btcKeyGenSessionProvider = BtcSessionProvider(
        integrationHelper.accountHelper.registrationAccount,
        integrationHelper.irohaAPI
    )

    private val registrationKeyPair =
        ModelUtil.loadKeypair(
            btcGenerationConfig.registrationAccount.pubkeyPath,
            btcGenerationConfig.registrationAccount.privkeyPath
        ).fold({ keypair ->
            keypair
        }, { ex -> throw ex })

    val registrationCredential =
        IrohaCredential(btcGenerationConfig.registrationAccount.accountId, registrationKeyPair)

    private val mstRegistrationKeyPair =
        ModelUtil.loadKeypair(
            btcGenerationConfig.mstRegistrationAccount.pubkeyPath,
            btcGenerationConfig.mstRegistrationAccount.privkeyPath
        ).fold({ keypair ->
            keypair
        }, { ex -> throw ex })

    private val sessionConsumer =
        IrohaConsumerImpl(registrationCredential, integrationHelper.irohaAPI)

    private val multiSigConsumer = IrohaConsumerImpl(
        IrohaCredential(btcGenerationConfig.mstRegistrationAccount.accountId, mstRegistrationKeyPair),
        integrationHelper.irohaAPI
    )

    fun btcPublicKeyProvider(): BtcPublicKeyProvider {
        val file = File(btcGenerationConfig.btcWalletFilePath)
        val wallet = Wallet.loadFromFile(file)
        val walletFile = WalletFile(wallet, file)
        val notaryPeerListProvider = NotaryPeerListProviderImpl(
            integrationHelper.irohaAPI,
            registrationCredential,
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

    private val irohaListener = IrohaChainListener(
        btcGenerationConfig.iroha.hostname,
        btcGenerationConfig.iroha.port,
        registrationCredential
    )

    val btcAddressGenerationInitialization = BtcAddressGenerationInitialization(
        registrationCredential,
        integrationHelper.irohaAPI,
        btcGenerationConfig,
        btcPublicKeyProvider(),
        irohaListener
    )

    override fun close() {
        integrationHelper.close()
        irohaListener.close()
    }
}
