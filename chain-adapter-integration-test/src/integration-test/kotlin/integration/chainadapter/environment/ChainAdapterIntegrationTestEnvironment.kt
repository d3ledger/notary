package integration.chainadapter.environment

import com.d3.chainadapter.CHAIN_ADAPTER_SERVICE_NAME
import com.d3.chainadapter.adapter.ChainAdapter
import com.d3.chainadapter.provider.FileBasedLastReadBlockProvider
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.IrohaChainListener
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import com.d3.commons.util.createPrettySingleThreadPool
import com.d3.commons.util.getRandomString
import integration.helper.ChainAdapterConfigHelper
import integration.helper.IrohaIntegrationHelperUtil
import io.grpc.ManagedChannelBuilder
import iroha.protocol.Commands
import jp.co.soramitsu.iroha.java.IrohaAPI
import java.io.Closeable

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

    val consumerExecutorService =
        createPrettySingleThreadPool(CHAIN_ADAPTER_SERVICE_NAME, "iroha-blocks-consumer")

    private val chainAdapterConfigHelper = ChainAdapterConfigHelper()

    val rmqConfig = chainAdapterConfigHelper.createRmqConfig()

    private val irohaCredential = rmqConfig.irohaCredential

    private val keyPair =
        ModelUtil.loadKeypair(irohaCredential.pubkeyPath, irohaCredential.privkeyPath).get()

    /**
     * It's essential to handle blocks in this service one-by-one.
     * This is why we explicitly set single threaded executor.
     */
    private val irohaAPI by lazy {
        val irohaAPI = IrohaAPI(rmqConfig.iroha.hostname, rmqConfig.iroha.port)
        irohaAPI.setChannelForStreamingQueryStub(
            ManagedChannelBuilder.forAddress(
                rmqConfig.iroha.hostname, rmqConfig.iroha.port
            ).executor(
                createPrettySingleThreadPool(
                    CHAIN_ADAPTER_SERVICE_NAME,
                    "iroha-chain-listener"
                )
            ).usePlaintext().build()
        )
    }

    private val queryHelper = IrohaQueryHelperImpl(
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
        queryHelper,
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

    override fun close() {
        integrationTestHelper.close()
        consumerExecutorService.shutdownNow()
        adapter.close()
    }
}
