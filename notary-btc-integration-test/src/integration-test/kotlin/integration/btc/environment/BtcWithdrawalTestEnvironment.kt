package integration.btc.environment

import com.d3.btc.handler.NewBtcClientRegistrationHandler
import com.d3.btc.helper.address.outPutToBase58Address
import com.d3.btc.peer.SharedPeerGroup
import com.d3.btc.provider.BtcChangeAddressProvider
import com.d3.btc.provider.BtcRegisteredAddressesProvider
import com.d3.btc.provider.network.BtcNetworkConfigProvider
import com.d3.btc.provider.network.BtcRegTestConfigProvider
import com.d3.btc.wallet.WalletInitializer
import com.d3.btc.wallet.loadAutoSaveWallet
import com.d3.btc.withdrawal.BTC_WITHDRAWAL_SERVICE_NAME
import com.d3.btc.withdrawal.config.BtcWithdrawalConfig
import com.d3.btc.withdrawal.handler.NewChangeAddressHandler
import com.d3.btc.withdrawal.handler.NewConsensusDataHandler
import com.d3.btc.withdrawal.handler.NewSignatureEventHandler
import com.d3.btc.withdrawal.handler.NewTransferHandler
import com.d3.btc.withdrawal.init.BtcWithdrawalInitialization
import com.d3.btc.withdrawal.provider.WithdrawalConsensusProvider
import com.d3.btc.withdrawal.service.BtcRollbackService
import com.d3.btc.withdrawal.service.WithdrawalTransferService
import com.d3.btc.withdrawal.statistics.WithdrawalStatistics
import com.d3.btc.withdrawal.transaction.*
import com.d3.commons.config.BitcoinConfig
import com.d3.commons.config.RMQConfig
import com.d3.commons.config.getConfigFolder
import com.d3.commons.config.loadRawConfigs
import com.d3.commons.model.IrohaCredential
import com.d3.commons.provider.NotaryPeerListProviderImpl
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.createPrettySingleThreadPool
import com.rabbitmq.client.ConnectionFactory
import integration.helper.BtcIntegrationHelperUtil
import io.grpc.ManagedChannelBuilder
import jp.co.soramitsu.iroha.java.IrohaAPI
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionOutput
import org.bitcoinj.wallet.Wallet
import java.io.Closeable
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Bitcoin withdrawal service testing environment
 */
class BtcWithdrawalTestEnvironment(
    private val integrationHelper: BtcIntegrationHelperUtil,
    testName: String = "default_test_name",
    val btcWithdrawalConfig: BtcWithdrawalConfig = integrationHelper.configHelper.createBtcWithdrawalConfig(
        testName
    ),
    withdrawalCredential: IrohaCredential =
        IrohaCredential(
            btcWithdrawalConfig.withdrawalCredential.accountId, ModelUtil.loadKeypair(
                btcWithdrawalConfig.withdrawalCredential.pubkeyPath,
                btcWithdrawalConfig.withdrawalCredential.privkeyPath
            ).get()
        )
) : Closeable {

    val createdTransactions = ConcurrentHashMap<String, Pair<Long, Transaction>>()

    /**
     * It's essential to handle blocks in this service one-by-one.
     * This is why we explicitly set single threaded executor.
     */
    private val executor =
        createPrettySingleThreadPool(BTC_WITHDRAWAL_SERVICE_NAME, "iroha-chain-listener")

    val rmqConfig =
        loadRawConfigs("rmq", RMQConfig::class.java, "${getConfigFolder()}/rmq.properties")

    /**
     * Binds RabbitMQ queue with exchange.
     * @param queue - queue to bind with exchange
     * @param exchange - exchange to bind with queue
     */
    fun bindQueueWithExchange(queue: String, exchange: String) {
        val factory = ConnectionFactory()
        factory.host = rmqConfig.host
        factory.newConnection().use { connection ->
            connection.createChannel().use { channel ->
                channel.exchangeDeclare(rmqConfig.irohaExchange, "fanout", true)
                channel.queueDeclare(queue, true, false, false, null)
                channel.queueBind(queue, exchange, "")
            }
        }
    }

    val transferWallet by lazy {
        loadAutoSaveWallet(btcWithdrawalConfig.btcTransfersWalletPath)
    }

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

    private val signaturesCollectorKeypair = ModelUtil.loadKeypair(
        btcWithdrawalConfig.signatureCollectorCredential.pubkeyPath,
        btcWithdrawalConfig.signatureCollectorCredential.privkeyPath
    ).fold({ keypair -> keypair }, { ex -> throw ex })

    private val signaturesCollectorCredential =
        IrohaCredential(
            btcWithdrawalConfig.signatureCollectorCredential.accountId,
            signaturesCollectorKeypair
        )

    private val btcConsensusKeyPair = ModelUtil.loadKeypair(
        btcWithdrawalConfig.btcConsensusCredential.pubkeyPath,
        btcWithdrawalConfig.btcConsensusCredential.privkeyPath
    ).fold({ keypair -> keypair }, { ex -> throw ex })

    private val btcConsensusCredential =
        IrohaCredential(btcWithdrawalConfig.btcConsensusCredential.accountId, btcConsensusKeyPair)

    private val withdrawalIrohaConsumer = IrohaConsumerImpl(
        withdrawalCredential,
        irohaApi
    )

    private val signaturesCollectorIrohaConsumer = IrohaConsumerImpl(
        signaturesCollectorCredential,
        irohaApi
    )

    private val btcConsensusIrohaConsumer = IrohaConsumerImpl(btcConsensusCredential, irohaApi)

    val btcRegisteredAddressesProvider = BtcRegisteredAddressesProvider(
        integrationHelper.queryAPI,
        btcWithdrawalConfig.registrationCredential.accountId,
        btcWithdrawalConfig.notaryCredential.accountId
    )

    private val btcNetworkConfigProvider = BtcRegTestConfigProvider()

    val btcChangeAddressProvider = BtcChangeAddressProvider(
        integrationHelper.queryAPI,
        btcWithdrawalConfig.mstRegistrationAccount,
        btcWithdrawalConfig.changeAddressesStorageAccount
    )

    private val walletInitializer by lazy {
        WalletInitializer(btcRegisteredAddressesProvider, btcChangeAddressProvider)
    }

    private val peerGroup by lazy {
        integrationHelper.getPeerGroup(
            transferWallet,
            btcNetworkConfigProvider,
            btcWithdrawalConfig.bitcoin.blockStoragePath,
            BitcoinConfig.extractHosts(btcWithdrawalConfig.bitcoin),
            walletInitializer
        )
    }

    val transactionHelper =
        BlackListableTransactionHelper(
            transferWallet,
            peerGroup,
            btcNetworkConfigProvider,
            btcRegisteredAddressesProvider,
            btcChangeAddressProvider
        )
    private val transactionCreator =
        TransactionCreator(btcChangeAddressProvider, btcNetworkConfigProvider, transactionHelper)
    private val transactionSigner =
        TransactionSigner(btcRegisteredAddressesProvider, btcChangeAddressProvider)
    val signCollector =
        SignCollector(
            signaturesCollectorCredential,
            signaturesCollectorIrohaConsumer,
            irohaApi,
            transactionSigner
        )

    private val withdrawalStatistics = WithdrawalStatistics.create()
    private val notaryPeerListProvider = NotaryPeerListProviderImpl(
        integrationHelper.queryAPI,
        btcWithdrawalConfig.notaryListStorageAccount,
        btcWithdrawalConfig.notaryListSetterAccount
    )
    private val btcRollbackService =
        BtcRollbackService(withdrawalIrohaConsumer)
    val unsignedTransactions = UnsignedTransactions(signCollector)
    private val withdrawalConsensusProvider = WithdrawalConsensusProvider(
        btcConsensusCredential,
        btcConsensusIrohaConsumer,
        notaryPeerListProvider,
        transactionHelper,
        btcWithdrawalConfig
    )
    private val newChangeAddressHandler
            by lazy { NewChangeAddressHandler(transferWallet, btcNetworkConfigProvider) }
    private val newTransferHandler =
        NewTransferHandler(
            withdrawalStatistics,
            btcWithdrawalConfig,
            withdrawalConsensusProvider,
            btcRollbackService,
            transactionHelper
        )
    val withdrawalTransferService = WithdrawalTransferService(
        withdrawalStatistics,
        btcWithdrawalConfig,
        transactionCreator,
        signCollector,
        unsignedTransactions,
        transactionHelper,
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

    private val newConsensusDataHandler = NewConsensusDataHandler(withdrawalTransferService)

    val btcWithdrawalInitialization by lazy {
        BtcWithdrawalInitialization(
            btcWithdrawalConfig,
            peerGroup,
            transferWallet,
            btcChangeAddressProvider,
            btcNetworkConfigProvider,
            newSignatureEventHandler,
            NewBtcClientRegistrationHandler(btcNetworkConfigProvider),
            newTransferHandler,
            newChangeAddressHandler,
            newConsensusDataHandler,
            btcRegisteredAddressesProvider,
            rmqConfig
        )
    }

    fun getLastCreatedTxHash() =
        createdTransactions.maxBy { createdTransactionEntry -> createdTransactionEntry.value.first }!!.key

    fun getLastCreatedTx() =
        createdTransactions.maxBy { createdTransactionEntry -> createdTransactionEntry.value.first }!!.value.second

    class BlackListableTransactionHelper(
        transferWallet: Wallet,
        peerGroup: SharedPeerGroup,
        btcNetworkConfigProvider: BtcNetworkConfigProvider,
        btcRegisteredAddressesProvider: BtcRegisteredAddressesProvider,
        btcChangeAddressProvider: BtcChangeAddressProvider
    ) : TransactionHelper(
        transferWallet,
        peerGroup,
        btcNetworkConfigProvider,
        btcRegisteredAddressesProvider,
        btcChangeAddressProvider
    ) {
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
        override fun isAvailableOutput(
            availableAddresses: Set<String>,
            output: TransactionOutput
        ): Boolean {
            val btcAddress = outPutToBase58Address(output)
            return availableAddresses.contains(btcAddress) && !btcAddressBlackList.contains(
                btcAddress
            )
        }
    }

    override fun close() {
        irohaApi.close()
        integrationHelper.close()
        executor.shutdownNow()
        File(btcWithdrawalConfig.bitcoin.blockStoragePath).deleteRecursively()
        btcWithdrawalInitialization.close()
    }
}
