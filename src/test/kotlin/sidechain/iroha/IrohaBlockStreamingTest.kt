package sidechain.iroha

import com.github.kittinunf.result.map
import config.ConfigKeys
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import notary.CONFIG
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
 * Note: Requires Iroha is runnig.
 */
class IrohaBlockStreamingTest {

    /**
     * @given
     * @when
     * @then
     */
    @Disabled
    @Test
    fun irohaStreamingTest() {
        System.loadLibrary("irohajava")

        val irohaHost = CONFIG[ConfigKeys.testIrohaHostname]
        val irohaPort = CONFIG[ConfigKeys.testIrohaPort]

        val admin = CONFIG[ConfigKeys.testIrohaAccount]
        val keypair = ModelUtil.loadKeypair(
            CONFIG[ConfigKeys.testPubkeyPath],
            CONFIG[ConfigKeys.testPrivkeyPath]
        ).get()


        var cmds = listOf<iroha.protocol.Commands.Command>()

        IrohaChainListener(admin, keypair).getBlockObservable()
            .map {
                it.map { block ->
                    cmds = block.payload.transactionsList
                        .flatMap {
                            it.payload.reducedPayload.commandsList
                        }
                }.subscribeOn(Schedulers.io()).subscribe()
            }

        val utx = getModelTransactionBuilder()
            .creatorAccountId(admin)
            .createdTime(getCurrentTime())
            .setAccountDetail(admin, "test", "test")
            .build()

        val tx = prepareTransaction(utx, keypair)
        IrohaNetworkImpl(irohaHost, irohaPort).sendAndCheck(tx, utx.hash())
        runBlocking {
            delay(5000, TimeUnit.MILLISECONDS)
        }

        assertEquals(1, cmds.size)
        assertEquals(admin, cmds.first().setAccountDetail.accountId)
        assertEquals("test", cmds.first().setAccountDetail.key)
        assertEquals("test", cmds.first().setAccountDetail.value)
    }
}
