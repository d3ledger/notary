package integration.chainadapter.environment

import com.d3.chainadapter.adapter.ChainAdapter
import com.d3.chainadapter.provider.FileBasedLastReadBlockProvider
import integration.helper.ChainAdapterConfigHelper
import integration.helper.IrohaIntegrationHelperUtil
import io.grpc.ManagedChannelBuilder
import iroha.protocol.Commands
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import model.IrohaCredential
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.util.ModelUtil
import util.getRandomString
import java.io.Closeable
import java.util.concurrent.Executors

/**
 * Chain adapter test environment
 */
class ChainAdapterIntegrationTestEnvironment : Closeable {

    private val dummyValue = String.getRandomString(5)

    val integrationTestHelper = IrohaIntegrationHelperUtil()

    init {
        /*
         Trigger tester account creation. We need this account to create dummy Iroha transactions.
         */
        integrationTestHelper.accountHelper.irohaConsumer
    }

    val consumerExecutorService = Executors.newSingleThreadExecutor()

    private val chainAdapterConfigHelper = ChainAdapterConfigHelper()

    val rmqConfig = chainAdapterConfigHelper.createRmqConfig()

    private val irohaCredential = rmqConfig.irohaCredential

    private val keyPair = ModelUtil.loadKeypair(irohaCredential.pubkeyPath, irohaCredential.privkeyPath).get()

    /**
     * It's essential to handle blocks in this service one-by-one.
     * This is why we explicitly set single threaded executor.
     */
    private val irohaAPI by lazy {
        val irohaAPI = IrohaAPI(rmqConfig.iroha.hostname, rmqConfig.iroha.port)
        irohaAPI.setChannelForStreamingQueryStub(
            ManagedChannelBuilder.forAddress(
                rmqConfig.iroha.hostname, rmqConfig.iroha.port
            ).executor(Executors.newSingleThreadExecutor()).usePlaintext().build()
        )
    }

    private val queryAPI =
        QueryAPI(
            irohaAPI,
            irohaCredential.accountId,
            keyPair
        )

    private val irohaChainListener = IrohaChainListener(
        irohaAPI,
        IrohaCredential(irohaCredential.accountId, keyPair)
    )

    val lastReadBlockProvider = FileBasedLastReadBlockProvider(rmqConfig)

    val adapter = ChainAdapter(
        rmqConfig,
        queryAPI,
        irohaChainListener,
        lastReadBlockProvider
    )

    /**
     * Creates dummy transaction
     */
    fun createDummyTransaction(testKey: String = dummyValue) {
        integrationTestHelper.createDummyTransaction(testKey, dummyValue)
    }

    /**
     * Checks if command is dummy
     */
    fun isDummyCommand(command: Commands.Command): Boolean {
        return command.hasSetAccountDetail() && command.setAccountDetail.value == dummyValue
    }

    // Checks if read blocks list is ordered
    fun isOrdered(readBlocks: List<Long>): Boolean {
        if (readBlocks.isEmpty() || readBlocks.size == 1) {
            return true
        }
        for (i in 1..(readBlocks.size - 1)) {
            if (readBlocks[i - 1] > readBlocks[i]) {
                return false
            }
        }
        return true
    }

    override fun close() {
        integrationTestHelper.close()
        consumerExecutorService.shutdownNow()
        adapter.close()
    }
}
