package notary


import io.reactivex.rxkotlin.toObservable
import io.reactivex.schedulers.Schedulers
import jp.co.soramitsu.iroha.ModelBlocksQueryBuilder
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import sidechain.iroha.util.*
import java.math.BigInteger
import java.util.concurrent.TimeUnit



class IrohaBlockEmitterTest {
    @Disabled
    @Test
    fun irohaStreamingTest() {
        System.loadLibrary("irohajava")

        println(System.getProperty("java.library.path"))
        val admin = "admin@notary"
        val keypair = getKeys("deploy/iroha/keys", admin)
        val utx = getModelTransactionBuilder()
            .creatorAccountId(admin)
            .createdTime(getCurrentTime())
            .setAccountDetail(admin, "key", "value")
            .build()

        val tx = prepareTransaction(utx, keypair)
        val cmdStub = getCommandStub()


        val uquery = ModelBlocksQueryBuilder()
            .creatorAccountId(admin)
            .createdTime(getCurrentTime())
            .queryCounter(BigInteger.valueOf(1))
            .build()

        val query = prepareBlocksQuery(uquery, keypair)
        val stub = getQueryStub()
        var cmds = listOf<iroha.protocol.Commands.Command>()
        val obs = stub.fetchCommits(query).toObservable()
        obs.subscribeOn(Schedulers.computation()).subscribe {
            val block = it
            val resp = block.blockResponse
            cmds = resp.block.payload.transactionsList
                .flatMap { it.payload.commandsList }

        }

        cmdStub.torii(tx)
        runBlocking {
            delay(5000, TimeUnit.MILLISECONDS)
        }
        assertEquals(1, cmds.size)
        assertEquals("key", cmds.first().setAccountDetail.key)
        assertEquals("value", cmds.first().setAccountDetail.value)


    }
}
