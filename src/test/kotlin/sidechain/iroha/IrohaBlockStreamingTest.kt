package notary

import com.github.kittinunf.result.success
import config.ConfigKeys
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.ModelUtil.getCommandStub
import sidechain.iroha.util.ModelUtil.getCurrentTime
import sidechain.iroha.util.ModelUtil.getModelTransactionBuilder
import sidechain.iroha.util.ModelUtil.prepareTransaction
import sidechain.iroha.util.toByteArray
import java.util.concurrent.TimeUnit

class IrohaBlockStreamingTest {
    @Disabled
    @Test
    fun irohaStreamingTest() {
        System.loadLibrary("irohajava")

        val address = "127.0.0.1:21917"
        val admin = CONFIG[ConfigKeys.testIrohaAccount]
        val keypair = ModelUtil.loadKeypair(
            CONFIG[ConfigKeys.testPubkeyPath],
            CONFIG[ConfigKeys.testPrivkeyPath]
        ).get()

        val utx = getModelTransactionBuilder()
            .creatorAccountId(admin)
            .createdTime(getCurrentTime())
            .addPeer(address, keypair.publicKey())
            .build()

        val tx = prepareTransaction(utx, keypair)
        val cmdStub = getCommandStub()

        var cmds = listOf<iroha.protocol.Commands.Command>()

        IrohaChainListener().getBlockObservable()
            .success {
                it.subscribeOn(Schedulers.computation()).subscribe { block ->
                    cmds = block.payload.transactionsList
                        .flatMap { it.payload.reducedPayload.commandsList }
                }
            }


        cmdStub.torii(tx)
        runBlocking {
            delay(5000, TimeUnit.MILLISECONDS)
        }
        assertEquals(1, cmds.size)
        assertEquals(address, cmds.first().addPeer.peer.address)
        assertArrayEquals(
            keypair.publicKey().blob().toByteArray(),
            cmds.first().addPeer.peer.peerKey.toByteArray()
        )


    }
}
