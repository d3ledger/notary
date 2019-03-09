package integration.btc.environment

import com.d3.btc.generation.config.BtcAddressGenerationConfig
import com.d3.btc.generation.init.BtcAddressGenerationInitialization
import com.d3.btc.generation.trigger.AddressGenerationTrigger
import com.d3.btc.provider.BtcFreeAddressesProvider
import com.d3.btc.provider.BtcRegisteredAddressesProvider
import com.d3.btc.provider.address.BtcAddressesProvider
import com.d3.btc.provider.generation.BtcPublicKeyProvider
import com.d3.btc.provider.generation.BtcSessionProvider
import com.d3.btc.provider.network.BtcRegTestConfigProvider
import integration.helper.BtcIntegrationHelperUtil
import io.grpc.ManagedChannelBuilder
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import com.d3.commons.model.IrohaCredential
import org.bitcoinj.wallet.Wallet
import com.d3.commons.provider.NotaryPeerListProviderImpl
import com.d3.commons.provider.TriggerProvider
import com.d3.commons.sidechain.iroha.IrohaChainListener
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import java.io.Closeable
import java.io.File
import java.util.concurrent.Executors

//How many addresses to generate at initial phase
private const val INIT_ADDRESSES = 3

/**
 * Bitcoin address generation service testing environment
 */
class BtcAddressGenerationTestEnvironment(
    private val integrationHelper: BtcIntegrationHelperUtil,
    val testName: String = "test",
    val btcGenerationConfig: BtcAddressGenerationConfig =
        integrationHelper.configHelper.createBtcAddressGenerationConfig(INIT_ADDRESSES, testName),
    mstRegistrationCredential: IrohaCredential = IrohaCredential(
        btcGenerationConfig.mstRegistrationAccount.accountId,
        ModelUtil.loadKeypair(
            btcGenerationConfig.mstRegistrationAccount.pubkeyPath,
            btcGenerationConfig.mstRegistrationAccount.privkeyPath
        ).get()
    )
) : Closeable {

    private val keysWallet = Wallet.loadFromFile(File(btcGenerationConfig.btcKeysWalletPath))
    /**
     * It's essential to handle blocks in this service one-by-one.
     * This is why we explicitly set single threaded executor.
     */
    private val executor = Executors.newSingleThreadExecutor()

    private val irohaApi by lazy {
        val irohaAPI = IrohaAPI(
            btcGenerationConfig.iroha.hostname,
            btcGenerationConfig.iroha.port
        )

        irohaAPI.setChannelForStreamingQueryStub(
            ManagedChannelBuilder.forAddress(
                btcGenerationConfig.iroha.hostname,
                btcGenerationConfig.iroha.port
            ).executor(executor).usePlaintext().build()
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

    private val sessionConsumer =
        IrohaConsumerImpl(registrationCredential, irohaApi)

    private val multiSigConsumer = IrohaConsumerImpl(
        mstRegistrationCredential,
        irohaApi
    )

    private val registrationQueryAPI = QueryAPI(
        irohaApi,
        registrationCredential.accountId,
        registrationCredential.keyPair
    )

    private fun btcPublicKeyProvider(): BtcPublicKeyProvider {
        val notaryPeerListProvider = NotaryPeerListProviderImpl(
            registrationQueryAPI,
            btcGenerationConfig.notaryListStorageAccount,
            btcGenerationConfig.notaryListSetterAccount
        )
        return BtcPublicKeyProvider(
            keysWallet,
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
        BtcFreeAddressesProvider(btcGenerationConfig.nodeId, btcAddressesProvider, btcRegisteredAddressesProvider)

    private val addressGenerationTrigger = AddressGenerationTrigger(
        btcKeyGenSessionProvider,
        triggerProvider,
        btcFreeAddressesProvider,
        IrohaConsumerImpl(registrationCredential, irohaApi)
    )

    val btcAddressGenerationInitialization = BtcAddressGenerationInitialization(
        keysWallet,
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
        executor.shutdownNow()
        irohaApi.close()
        irohaListener.close()
    }
}
