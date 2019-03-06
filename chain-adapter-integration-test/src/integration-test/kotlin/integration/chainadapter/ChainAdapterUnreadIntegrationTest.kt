package integration.chainadapter

import integration.chainadapter.environment.ChainAdapterIntegrationTestEnvironment
import mu.KLogging
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import sidechain.iroha.ReliableIrohaChainListener
import util.getRandomString
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChainAdapterUnreadIntegrationTest {

    private val environment = ChainAdapterIntegrationTestEnvironment()

    @AfterAll
    fun tearDown() {
        environment.close()
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given not running chain-adapter
     * @when 10 dummy transactions appear in Iroha blockchain before and after chain-adapter start
     * @then RabbitMQ consumer reads 20 transactions
     * in the same order as they were published
     */
    @Test
    fun testUnreadBlocksWerePublished() {
        val transactionsBeforeStart = 10
        val transactionsAfterStart = 10
        var transactionsCount = 0
        val queueName = String.getRandomString(5)
        val consumedTransactions = Collections.synchronizedList(ArrayList<Long>())
        ReliableIrohaChainListener(
            environment.rmqConfig,
            queueName,
            { block, _ ->
                block.blockV1.payload.transactionsList.forEach { tx ->
                    tx.payload.reducedPayload.commandsList.forEach { command ->
                        if (environment.isDummyCommand(command)) {
                            // Collect dummy transactions
                            // Key is number of transaction
                            consumedTransactions.add(command.setAccountDetail.key.toLong())
                        }
                    }
                }
            },
            environment.consumerExecutorService,
            true
        ).use { reliableChainListener ->
            // Start consuming
            reliableChainListener.getBlockObservable()
            // Before start
            logger.info { "Start send dummy transactions before service start" }
            for (transaction in 1..transactionsBeforeStart) {
                environment.createDummyTransaction(testKey = transactionsCount.toString())
                transactionsCount++
            }
            // Start
            environment.adapter.init()
            // After start
            logger.info { "Start send dummy transactions after service start" }
            for (transaction in 1..transactionsAfterStart) {
                environment.createDummyTransaction(testKey = transactionsCount.toString())
                transactionsCount++
            }
            logger.info { consumedTransactions }
            assertEquals(transactionsAfterStart + transactionsAfterStart, consumedTransactions.size)
            assertTrue(environment.isOrdered((consumedTransactions)))
            assertEquals(
                environment.adapter.getLastReadBlock(),
                environment.lastReadBlockProvider.getLastBlockHeight()
            )
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
