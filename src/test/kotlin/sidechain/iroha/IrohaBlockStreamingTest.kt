package sidechain.iroha

import com.github.kittinunf.result.map
import config.TestConfig
import config.loadConfigs
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.ModelUtil.getCurrentTime
import sidechain.iroha.util.ModelUtil.getModelTransactionBuilder
import java.util.concurrent.TimeUnit

/**
 * Note: Requires Iroha is running.
 */
class IrohaBlockStreamingTest {

    /** Test configurations */
    val testConfig = loadConfigs("test", TestConfig::class.java, "/test.properties")

    /**
     * @given Iroha running
     * @when new tx is sent to Iroha
     * @then block arrived to IrohaListener
     */
    @Disabled
    @Test
    fun irohaStreamingTest() {
        System.loadLibrary("irohajava")

        val creator = testConfig.iroha.creator
        val keypair = ModelUtil.loadKeypair(
            testConfig.iroha.pubkeyPath,
            testConfig.iroha.privkeyPath
        ).get()


        var cmds = listOf<iroha.protocol.Commands.Command>()

        IrohaChainListener(
            testConfig.iroha.hostname,
            testConfig.iroha.port,
            creator, keypair
        ).getBlockObservable()
            .map {
                it.map { block ->
                    cmds = block.payload.transactionsList
                        .flatMap {
                            it.payload.reducedPayload.commandsList
                        }
                }.subscribeOn(Schedulers.io()).subscribe()
            }

        val utx = getModelTransactionBuilder()
            .creatorAccountId(creator)
            .createdTime(getCurrentTime())
            .setAccountDetail(creator, "test", "test")
            .build()

        IrohaConsumerImpl(testConfig.iroha).sendAndCheck(utx)
        runBlocking {
            delay(5000, TimeUnit.MILLISECONDS)
        }

        assertEquals(1, cmds.size)
        assertEquals(creator, cmds.first().setAccountDetail.accountId)
        assertEquals("test", cmds.first().setAccountDetail.key)
        assertEquals("test", cmds.first().setAccountDetail.value)
    }
}
