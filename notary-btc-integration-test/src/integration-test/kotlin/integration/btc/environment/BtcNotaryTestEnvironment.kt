package integration.btc.environment

import com.d3.btc.deposit.BTC_DEPOSIT_SERVICE_NAME
import com.d3.btc.deposit.config.BtcDepositConfig
import com.d3.btc.deposit.config.depositConfig
import com.d3.btc.deposit.init.BtcNotaryInitialization
import com.d3.btc.deposit.service.BtcWalletListenerRestartService
import com.d3.btc.handler.NewBtcClientRegistrationHandler
import com.d3.btc.listener.NewBtcClientRegistrationListener
import com.d3.btc.peer.SharedPeerGroup
import com.d3.btc.provider.BtcChangeAddressProvider
import com.d3.btc.provider.BtcRegisteredAddressesProvider
import com.d3.btc.provider.network.BtcRegTestConfigProvider
import com.d3.btc.wallet.WalletInitializer
import com.d3.btc.wallet.loadAutoSaveWallet
import com.d3.commons.config.BitcoinConfig
import com.d3.commons.model.IrohaCredential
import com.d3.commons.notary.NotaryImpl
import com.d3.commons.sidechain.SideChainEvent
import com.d3.commons.sidechain.iroha.IrohaChainListener
import com.d3.commons.sidechain.iroha.consumer.MultiSigIrohaConsumer
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.createPrettySingleThreadPool
import integration.helper.BtcIntegrationHelperUtil
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import org.bitcoinj.wallet.Wallet
import java.io.Closeable
import java.io.File


/**
 * Bitcoin notary service testing environment
 */
class BtcNotaryTestEnvironment(
    private val integrationHelper: BtcIntegrationHelperUtil,
    testName: String = "",
    val notaryConfig: BtcDepositConfig = integrationHelper.configHelper.createBtcDepositConfig(testName),
    notaryCredential: IrohaCredential = IrohaCredential(
        notaryConfig.notaryCredential.accountId,
        ModelUtil.loadKeypair(notaryConfig.notaryCredential.pubkeyPath, notaryConfig.notaryCredential.privkeyPath).get()
    )
) : Closeable {

    private val irohaAPI = IrohaAPI(notaryConfig.iroha.hostname, notaryConfig.iroha.port)

    private val queryAPI = QueryAPI(irohaAPI, notaryCredential.accountId, notaryCredential.keyPair)

    private val btcRegisteredAddressesProvider = BtcRegisteredAddressesProvider(
        queryAPI,
        notaryConfig.registrationAccount,
        notaryConfig.notaryCredential.accountId
    )

    val btcAddressGenerationConfig = integrationHelper.configHelper.createBtcAddressGenerationConfig(0)

    private val btcNetworkConfigProvider = BtcRegTestConfigProvider()

    val irohaChainListener = IrohaChainListener(
        notaryConfig.iroha.hostname,
        notaryConfig.iroha.port,
        notaryCredential
    )

    private val newBtcClientRegistrationListener =
        NewBtcClientRegistrationListener(
            NewBtcClientRegistrationHandler(btcNetworkConfigProvider),
            createPrettySingleThreadPool(BTC_DEPOSIT_SERVICE_NAME, "reg-clients-listener")
        )

    private val transferWallet by lazy {
        loadAutoSaveWallet(notaryConfig.btcTransferWalletPath)
    }

    private val peerGroup by lazy {
        createPeerGroup(transferWallet)
    }

    private val btcChangeAddressProvider = BtcChangeAddressProvider(
        queryAPI, depositConfig.mstRegistrationAccount,
        depositConfig.changeAddressesStorageAccount
    )

    private val walletInitializer by lazy {
        WalletInitializer(btcRegisteredAddressesProvider, btcChangeAddressProvider)
    }

    fun createPeerGroup(transferWallet: Wallet): SharedPeerGroup {
        return integrationHelper.getPeerGroup(
            transferWallet,
            btcNetworkConfigProvider,
            notaryConfig.bitcoin.blockStoragePath,
            BitcoinConfig.extractHosts(notaryConfig.bitcoin),
            walletInitializer
        )
    }

    private val confidenceExecutorService =
        createPrettySingleThreadPool(BTC_DEPOSIT_SERVICE_NAME, "tx-confidence-listener")

    private val btcEventsSource = PublishSubject.create<SideChainEvent.PrimaryBlockChainEvent>()

    private val btcEventsObservable: Observable<SideChainEvent.PrimaryBlockChainEvent> = btcEventsSource

    private val notary =
        NotaryImpl(MultiSigIrohaConsumer(notaryCredential, irohaAPI), notaryCredential, btcEventsObservable)

    private val btcWalletListenerRestartService by lazy {
        BtcWalletListenerRestartService(
            depositConfig,
            confidenceExecutorService,
            peerGroup,
            btcEventsSource,
            btcRegisteredAddressesProvider
        )
    }

    val btcNotaryInitialization by lazy {
        BtcNotaryInitialization(
            peerGroup,
            transferWallet,
            notaryConfig,
            notary,
            btcRegisteredAddressesProvider,
            btcEventsSource,
            irohaChainListener,
            newBtcClientRegistrationListener,
            btcWalletListenerRestartService,
            confidenceExecutorService,
            btcNetworkConfigProvider
        )
    }

    override fun close() {
        integrationHelper.close()
        irohaAPI.close()
        confidenceExecutorService.shutdownNow()
        irohaChainListener.close()
        //Clear bitcoin blockchain folder
        File(notaryConfig.bitcoin.blockStoragePath).deleteRecursively()
        btcNotaryInitialization.close()
    }
}
