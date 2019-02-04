package integration.iroha

import com.github.kittinunf.result.map
import integration.helper.IrohaConfigHelper
import integration.helper.IrohaIntegrationHelperUtil
import io.reactivex.schedulers.Schedulers
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Transaction
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaConsumerImpl
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

    private val listener = IrohaChainListener(
        testConfig.iroha.hostname,
        testConfig.iroha.port,
        testCredential
    )

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    @AfterAll
    fun dropDown() {
        integrationHelper.close()
        listener.close()
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
