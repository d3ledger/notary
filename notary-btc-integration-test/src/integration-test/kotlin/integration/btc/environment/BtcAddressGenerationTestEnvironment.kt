package integration.btc.environment

import generation.btc.init.BtcAddressGenerationInitialization
import generation.btc.trigger.AddressGenerationTrigger
import integration.helper.BtcIntegrationHelperUtil
import io.grpc.ManagedChannelBuilder
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import model.IrohaCredential
import org.bitcoinj.wallet.Wallet
import provider.NotaryPeerListProviderImpl
import provider.TriggerProvider
import provider.btc.address.BtcAddressesProvider
import provider.btc.address.BtcFreeAddressesProvider
import provider.btc.address.BtcRegisteredAddressesProvider
import provider.btc.generation.BtcPublicKeyProvider
import provider.btc.generation.BtcSessionProvider
import provider.btc.network.BtcRegTestConfigProvider
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil
import wallet.WalletFile
import java.io.Closeable
import java.io.File
import java.util.concurrent.Executors

/**
 * Bitcoin address generation service testing environment
 */
class BtcAddressGenerationTestEnvironment(private val integrationHelper: BtcIntegrationHelperUtil) : Closeable {

    val btcGenerationConfig =
        integrationHelper.configHelper.createBtcAddressGenerationConfig()

    private val irohaApi by lazy {
        val irohaAPI = IrohaAPI(
            btcGenerationConfig.iroha.hostname,
            btcGenerationConfig.iroha.port
        )
        /**
         * It's essential to handle blocks in this service one-by-one.
         * This is why we explicitly set single threaded executor.
         */
        irohaAPI.setChannelForStreamingQueryStub(
            ManagedChannelBuilder.forAddress(
                btcGenerationConfig.iroha.hostname,
                btcGenerationConfig.iroha.port
            ).executor(Executors.newSingleThreadExecutor()).usePlaintext().build()
        )
        irohaAPI
    }

    val triggerProvider = TriggerProvider(
        integrationHelper.testCredential,
        irohaApi,
        btcGenerationConfig.pubKeyTriggerAccount
    )
    val btcKeyGenSessionProvider = BtcSessionProvider(
        integrationHelper.accountHelper.registrationAccount,
        irohaApi
    )

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
        IrohaConsumerImpl(registrationCredential, irohaApi)

    private val multiSigConsumer = IrohaConsumerImpl(
        IrohaCredential(btcGenerationConfig.mstRegistrationAccount.accountId, mstRegistrationKeyPair),
        irohaApi
    )

    private val registrationQueryAPI = QueryAPI(
        irohaApi,
        registrationCredential.accountId,
        registrationCredential.keyPair
    )

    private fun btcPublicKeyProvider(): BtcPublicKeyProvider {
        val file = File(btcGenerationConfig.btcWalletFilePath)
        val wallet = Wallet.loadFromFile(file)
        val walletFile = WalletFile(wallet, file)
        val notaryPeerListProvider = NotaryPeerListProviderImpl(
            registrationQueryAPI,
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
        irohaApi,
        registrationCredential
    )

    private val btcAddressesProvider = BtcAddressesProvider(
        registrationQueryAPI,
        btcGenerationConfig.mstRegistrationAccount.accountId,
        btcGenerationConfig.notaryAccount
    )

    private val btcRegisteredAddressesProvider = BtcRegisteredAddressesProvider(
        registrationQueryAPI,
        registrationCredential.accountId,
        btcGenerationConfig.notaryAccount
    )

    val btcFreeAddressesProvider =
        BtcFreeAddressesProvider(btcAddressesProvider, btcRegisteredAddressesProvider)

    private val addressGenerationTrigger = AddressGenerationTrigger(
        btcKeyGenSessionProvider,
        triggerProvider,
        btcFreeAddressesProvider,
        IrohaConsumerImpl(registrationCredential, irohaApi)
    )

    val btcAddressGenerationInitialization = BtcAddressGenerationInitialization(
        registrationQueryAPI,
        btcGenerationConfig,
        btcPublicKeyProvider(),
        irohaListener,
        addressGenerationTrigger
    )

    /**
     * Checks if enough free addresses were generated at initial phase
     * @throws IllegalStateException if not enough
     */
    fun checkIfAddressesWereGeneratedAtInitialPhase() {
        btcFreeAddressesProvider.getFreeAddresses()
            .fold({ freeAddresses ->
                if (freeAddresses.size < btcGenerationConfig.threshold) {
                    throw IllegalStateException(
                        "Generation service was not properly started." +
                                " Not enough address were generated at initial phase " +
                                "(${freeAddresses.size} out of ${btcGenerationConfig.threshold})."
                    )
                }
            }, { ex -> throw IllegalStateException("Cannot get free addresses", ex) })
    }

    override fun close() {
        integrationHelper.close()
        irohaListener.close()
    }
}
