package sidechain.iroha

import com.github.kittinunf.result.map
import config.loadConfigs
import integration.TestConfig
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.ModelUtil.getCurrentTime
import sidechain.iroha.util.ModelUtil.getModelTransactionBuilder
import sidechain.iroha.util.ModelUtil.prepareTransaction
import java.util.concurrent.TimeUnit

/**
 * Note: Requires Iroha is running.
 */
class IrohaBlockStreamingTest {

    val testConfig = loadConfigs("test", TestConfig::class.java)

    /**
     * @given Iroha running
     * @when new tx is sent to Iroha
     * @then block arrived to IrohaListener
     */
    @Disabled
    @Test
    fun irohaStreamingTest() {
        System.loadLibrary("irohajava")

        val irohaHost = testConfig.iroha.hostname
        val irohaPort = testConfig.iroha.port

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

        val tx = prepareTransaction(utx, keypair)
        IrohaNetworkImpl(irohaHost, irohaPort).sendAndCheck(tx, utx.hash())
        runBlocking {
            delay(5000, TimeUnit.MILLISECONDS)
        }

        assertEquals(1, cmds.size)
        assertEquals(creator, cmds.first().setAccountDetail.accountId)
        assertEquals("test", cmds.first().setAccountDetail.key)
        assertEquals("test", cmds.first().setAccountDetail.value)
    }
}
