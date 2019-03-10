package integration.chainadapter

import com.d3.commons.sidechain.iroha.ReliableIrohaChainListener
import com.d3.commons.util.getRandomString
import com.github.kittinunf.result.failure
import integration.chainadapter.environment.ChainAdapterIntegrationTestEnvironment
import mu.KLogging
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChainAdapterIntegrationTest {

    private lateinit var environment: ChainAdapterIntegrationTestEnvironment

    @BeforeEach
    fun setUp() {
        environment = ChainAdapterIntegrationTestEnvironment()
        environment.adapter.init().failure { ex -> throw ex }
    }

    @AfterEach
    fun tearDown() {
        environment.close()
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given running chain-adapter
     * @when new transactions appear in Iroha blockchain
     * @then RabbitMQ consumer reads new transactions
     * in the same order as they were published
     */
    @Test
    fun testRuntimeBlocksWerePublished() {
        val transactions = 10
        val queueName = String.getRandomString(5)
        val consumedBlocks = Collections.synchronizedList(ArrayList<Long>())
        ReliableIrohaChainListener(
            environment.rmqConfig,
            queueName,
            { block, _ -> consumedBlocks.add(block.blockV1.payload.height) },
            environment.consumerExecutorService,
            true
        ).use { reliableChainListener ->
            //Start consuming
            reliableChainListener.getBlockObservable()
            repeat(transactions) {
                environment.createDummyTransaction()
            }
            //Wait a little until consumed
            Thread.sleep(2_000)
            logger.info { consumedBlocks }
            assertEquals(transactions, consumedBlocks.size)
            assertEquals(consumedBlocks.sorted(), consumedBlocks)
            assertEquals(environment.adapter.getLastReadBlock(), environment.lastReadBlockProvider.getLastBlockHeight())
            assertEquals(consumedBlocks.last(), environment.adapter.getLastReadBlock())
        }
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given running chain-adapter
     * @when new transactions appear in Iroha blockchain, but consumer doesn't acknowledge it
     * @then RabbitMQ consumer reads the first transaction and stop consuming next transactions,
     * because the first transaction was not acknowledged
     */
    @Test
    fun testRuntimeBlocksNoAck() {
        val transactions = 10
        val queueName = String.getRandomString(5)
        val consumedBlocks = Collections.synchronizedList(ArrayList<Long>())
        ReliableIrohaChainListener(
            environment.rmqConfig,
            queueName,
            { block, _ -> consumedBlocks.add(block.blockV1.payload.height) },
            environment.consumerExecutorService,
            false
        ).use { reliableChainListener ->
            //Start consuming
            reliableChainListener.getBlockObservable()
            repeat(transactions) {
                environment.createDummyTransaction()
            }
            //Wait a little until consumed
            Thread.sleep(2_000)
            logger.info { consumedBlocks }
            assertEquals(1, consumedBlocks.size)
        }
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given running chain-adapter
     * @when new transactions appear in Iroha blockchain and consumer acknowledges every transaction manually
     * @then RabbitMQ consumer reads new transactions
     * in the same order as they were published
     */
    @Test
    fun testRuntimeBlocksManualAck() {
        val transactions = 10
        val queueName = String.getRandomString(5)
        val consumedBlocks = Collections.synchronizedList(ArrayList<Long>())
        ReliableIrohaChainListener(
            environment.rmqConfig,
            queueName,
            { block, ack ->
                consumedBlocks.add(block.blockV1.payload.height)
                ack()
            },
            environment.consumerExecutorService,
            false
        ).use { reliableChainListener ->
            //Start consuming
            reliableChainListener.getBlockObservable()
            repeat(transactions) {
                environment.createDummyTransaction()
            }
            //Wait a little until consumed
            Thread.sleep(2_000)
            logger.info { consumedBlocks }
            assertEquals(transactions, consumedBlocks.size)
            assertEquals(consumedBlocks.sorted(), consumedBlocks)
            assertEquals(environment.adapter.getLastReadBlock(), environment.lastReadBlockProvider.getLastBlockHeight())
            assertEquals(consumedBlocks.last(), environment.adapter.getLastReadBlock())
        }
    }


    /**
     * Logger
     */
    companion object : KLogging()
}
