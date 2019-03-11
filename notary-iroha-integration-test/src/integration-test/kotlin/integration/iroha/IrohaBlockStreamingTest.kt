package integration.iroha

import com.github.kittinunf.result.map
import com.d3.commons.config.RMQConfig
import com.d3.commons.config.getConfigFolder
import com.d3.commons.config.loadRawConfigs
import integration.helper.IrohaConfigHelper
import integration.helper.IrohaIntegrationHelperUtil
import io.reactivex.schedulers.Schedulers
import jp.co.soramitsu.iroha.java.Transaction
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import com.d3.commons.sidechain.iroha.ReliableIrohaChainListener
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.util.getRandomId
import java.time.Duration

/**
 * Note: Requires Iroha is running.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IrohaBlockStreamingTest {

    private val integrationHelper = IrohaIntegrationHelperUtil()

    /** Test configurations */
    private val testConfig = integrationHelper.testConfig

    private val testCredential = integrationHelper.testCredential

    private val creator = testCredential.accountId

    private val rmqConfig = loadRawConfigs("rmq", RMQConfig::class.java, "${getConfigFolder()}/rmq.properties")
    lateinit private var listener: ReliableIrohaChainListener

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    @BeforeEach
    fun setUp() {
        listener = ReliableIrohaChainListener(
            rmqConfig,
            String.getRandomId()
        )
    }

    @AfterEach
    fun dropDown() {
        listener.close()
    }

    @AfterAll
    fun tearDown() {
        integrationHelper.close()
    }

    /**
     * @given Iroha running
     * @when new tx is sent to Iroha
     * @then block arrived to IrohaListener
     */
    @Test
    fun irohaStreamingTest() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            var cmds = listOf<iroha.protocol.Commands.Command>()
            listener.getBlockObservable()
                .map { obs ->
                    obs.map { (block, _) ->
                        cmds = block.blockV1.payload.transactionsList
                            .flatMap {
                                it.payload.reducedPayload.commandsList
                            }
                    }.subscribeOn(Schedulers.io()).subscribe()
                }

            val utx = Transaction.builder(creator)
                .setAccountDetail(creator, "test", "test")
                .build()

            IrohaConsumerImpl(testCredential, integrationHelper.irohaAPI).send(utx)
            runBlocking {
                delay(5000)
            }

            assertEquals(creator, cmds.last().setAccountDetail.accountId)
            assertEquals("test", cmds.last().setAccountDetail.key)
            assertEquals("test", cmds.last().setAccountDetail.value)
        }
    }

    /**
     * @given Iroha running
     * @when new tx is sent to Iroha
     * @then block arrived to IrohaListener and returned as coroutine
     */
    @Test
    fun irohaGetBlockTest() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val block = GlobalScope.async {
                listener.getBlock()
            }

            val utx = Transaction.builder(creator)
                .setAccountDetail(creator, "test", "test")
                .build()

            IrohaConsumerImpl(testCredential, integrationHelper.irohaAPI).send(utx)


            val (bl, _) = runBlocking {
                block.await()
            }

            val cmds = bl.blockV1.payload.transactionsList
                .flatMap {
                    it.payload.reducedPayload.commandsList
                }
            assertEquals(1, cmds.size)
            assertEquals(creator, cmds.first().setAccountDetail.accountId)
            assertEquals("test", cmds.first().setAccountDetail.key)
            assertEquals("test", cmds.first().setAccountDetail.value)
        }
    }
}
