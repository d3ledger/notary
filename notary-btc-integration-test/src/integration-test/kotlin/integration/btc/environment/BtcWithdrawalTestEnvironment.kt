package integration.btc.environment

import com.d3.btc.fee.BtcFeeRateService
import com.d3.btc.handler.NewBtcClientRegistrationHandler
import com.d3.btc.helper.address.outPutToBase58Address
import com.d3.btc.provider.BtcRegisteredAddressesProvider
import com.d3.btc.provider.network.BtcNetworkConfigProvider
import com.d3.btc.provider.network.BtcRegTestConfigProvider
import config.BitcoinConfig
import integration.helper.BtcIntegrationHelperUtil
import io.grpc.ManagedChannelBuilder
import jp.co.soramitsu.iroha.java.IrohaAPI
import model.IrohaCredential
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionOutput
import org.bitcoinj.wallet.Wallet
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil
import withdrawal.btc.handler.NewFeeRateWasSetHandler
import withdrawal.btc.handler.NewSignatureEventHandler
import withdrawal.btc.handler.WithdrawalTransferEventHandler
import withdrawal.btc.init.BtcWithdrawalInitialization
import withdrawal.btc.provider.BtcChangeAddressProvider
import withdrawal.btc.provider.BtcWhiteListProvider
import withdrawal.btc.statistics.WithdrawalStatistics
import withdrawal.btc.transaction.*
import java.io.Closeable
import java.io.File
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Bitcoin withdrawal service testing environment
 */
class BtcWithdrawalTestEnvironment(private val integrationHelper: BtcIntegrationHelperUtil, testName: String = "") :
    Closeable {

    val createdTransactions = ConcurrentHashMap<String, Pair<Long, Transaction>>()

    val btcWithdrawalConfig = integrationHelper.configHelper.createBtcWithdrawalConfig(testName)

    /**
     * It's essential to handle blocks in this service one-by-one.
     * This is why we explicitly set single threaded executor.
     */
    private val executor = Executors.newSingleThreadExecutor()

    private val irohaApi by lazy {
        val irohaAPI = IrohaAPI(
            btcWithdrawalConfig.iroha.hostname,
            btcWithdrawalConfig.iroha.port
        )
        irohaAPI.setChannelForStreamingQueryStub(
            ManagedChannelBuilder.forAddress(
                btcWithdrawalConfig.iroha.hostname,
                btcWithdrawalConfig.iroha.port
            ).executor(executor).usePlaintext().build()
        )
        irohaAPI
    }

    private val withdrawalKeypair = ModelUtil.loadKeypair(
        btcWithdrawalConfig.withdrawalCredential.pubkeyPath,
        btcWithdrawalConfig.withdrawalCredential.privkeyPath
    ).fold({ keypair -> keypair }, { ex -> throw ex })

    private val btcFeeRateKeypair = ModelUtil.loadKeypair(
        btcWithdrawalConfig.btcFeeRateCredential.pubkeyPath,
        btcWithdrawalConfig.btcFeeRateCredential.privkeyPath
    ).fold({ keypair -> keypair }, { ex -> throw ex })

    private val withdrawalCredential =
        IrohaCredential(btcWithdrawalConfig.withdrawalCredential.accountId, withdrawalKeypair)

    private val btcFeeRateCredential =
        IrohaCredential(btcWithdrawalConfig.btcFeeRateCredential.accountId, btcFeeRateKeypair)

    private val signaturesCollectorKeypair = ModelUtil.loadKeypair(
        btcWithdrawalConfig.signatureCollectorCredential.pubkeyPath,
        btcWithdrawalConfig.signatureCollectorCredential.privkeyPath
    ).fold({ keypair -> keypair }, { ex -> throw ex })

    private val signaturesCollectorCredential =
        IrohaCredential(btcWithdrawalConfig.signatureCollectorCredential.accountId, signaturesCollectorKeypair)

    private val withdrawalIrohaConsumer = IrohaConsumerImpl(
        withdrawalCredential,
        irohaApi
    )

    private val signaturesCollectorIrohaConsumer = IrohaConsumerImpl(
        signaturesCollectorCredential,
        irohaApi
    )

    private val btcFeeRateConsumer = IrohaConsumerImpl(btcFeeRateCredential, irohaApi)

    private val irohaChainListener = IrohaChainListener(
        irohaApi,
        withdrawalCredential
    )

    val btcRegisteredAddressesProvider = BtcRegisteredAddressesProvider(
        integrationHelper.queryAPI,
        btcWithdrawalConfig.registrationCredential.accountId,
        btcWithdrawalConfig.notaryCredential.accountId
    )

    val btcNetworkConfigProvider = BtcRegTestConfigProvider()
    val btcChangeAddressProvider = BtcChangeAddressProvider(
        integrationHelper.queryAPI,
        btcWithdrawalConfig.mstRegistrationAccount,
        btcWithdrawalConfig.changeAddressesStorageAccount
    )

    val transactionHelper =
        BlackListableTransactionHelper(
            btcNetworkConfigProvider,
            btcRegisteredAddressesProvider,
            btcChangeAddressProvider
        )
    private val transactionCreator =
        TransactionCreator(btcChangeAddressProvider, btcNetworkConfigProvider, transactionHelper)
    private val transactionSigner = TransactionSigner(btcRegisteredAddressesProvider, btcChangeAddressProvider)
    val signCollector =
        SignCollector(
            signaturesCollectorCredential,
            signaturesCollectorIrohaConsumer,
            irohaApi,
            transactionSigner
        )

    private val withdrawalStatistics = WithdrawalStatistics.create()
    private val btcRollbackService = BtcRollbackService(withdrawalIrohaConsumer)
    val unsignedTransactions = UnsignedTransactions(signCollector)
    val withdrawalTransferEventHandler = WithdrawalTransferEventHandler(
        withdrawalStatistics,
        BtcWhiteListProvider(
            btcWithdrawalConfig.registrationCredential.accountId, integrationHelper.queryAPI
        ), btcWithdrawalConfig, transactionCreator, signCollector, unsignedTransactions
        , transactionHelper,
        btcRollbackService
    )
    val newSignatureEventHandler =
        NewSignatureEventHandler(
            withdrawalStatistics,
            signCollector,
            unsignedTransactions,
            transactionHelper,
            btcRollbackService
        )

    private val wallet by lazy {
        Wallet.loadFromFile(File(btcWithdrawalConfig.bitcoin.walletPath))
    }

    private val peerGroup by lazy {
        val peerGroup = integrationHelper.getPeerGroup(
            wallet,
            btcNetworkConfigProvider,
            btcWithdrawalConfig.bitcoin.blockStoragePath
        )
        BitcoinConfig.extractHosts(btcWithdrawalConfig.bitcoin).forEach { host ->
            peerGroup.addAddress(InetAddress.getByName(host))
        }
        peerGroup
    }

    private val btcFeeRateService =
        BtcFeeRateService(btcFeeRateConsumer, btcFeeRateCredential.accountId, integrationHelper.queryAPI)

    val btcWithdrawalInitialization by lazy {
        BtcWithdrawalInitialization(
            peerGroup,
            wallet,
            btcChangeAddressProvider,
            irohaChainListener,
            btcNetworkConfigProvider,
            withdrawalTransferEventHandler,
            newSignatureEventHandler,
            NewBtcClientRegistrationHandler(btcNetworkConfigProvider),
            NewFeeRateWasSetHandler(btcWithdrawalConfig.btcFeeRateCredential.accountId),
            btcRegisteredAddressesProvider,
            btcFeeRateService
        )
    }

    fun getLastCreatedTxHash() =
        createdTransactions.maxBy { createdTransactionEntry -> createdTransactionEntry.value.first }!!.key

    fun getLastCreatedTx() =
        createdTransactions.maxBy { createdTransactionEntry -> createdTransactionEntry.value.first }!!.value.second

    class BlackListableTransactionHelper(
        btcNetworkConfigProvider: BtcNetworkConfigProvider,
        btcRegisteredAddressesProvider: BtcRegisteredAddressesProvider,
        btcChangeAddressProvider: BtcChangeAddressProvider
    ) : TransactionHelper(btcNetworkConfigProvider, btcRegisteredAddressesProvider, btcChangeAddressProvider) {
        //Collection of "blacklisted" addresses. For testing purposes only
        private val btcAddressBlackList = HashSet<String>()

        /**
         * Adds address to black list. It makes given address money unable to spend
         * @param btcAddress - address to add in black list
         */
        fun addToBlackList(btcAddress: String) {
            btcAddressBlackList.add(btcAddress)
        }

        // Checks if transaction output was addressed to available address
        override fun isAvailableOutput(availableAddresses: Set<String>, output: TransactionOutput): Boolean {
            val btcAddress = outPutToBase58Address(output)
            return availableAddresses.contains(btcAddress) && !btcAddressBlackList.contains(btcAddress)
        }
    }

    override fun close() {
        integrationHelper.close()
        executor.shutdownNow()
        irohaChainListener.close()
        File(btcWithdrawalConfig.bitcoin.blockStoragePath).deleteRecursively()
        btcWithdrawalInitialization.close()
    }
}
