package notary


import io.grpc.ManagedChannelBuilder
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import iroha.protocol.Queries
import iroha.protocol.QueryServiceGrpc
import iroha.protocol.Responses
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.withTimeoutOrNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import sidechain.iroha.IrohaBlockEmitter
import sidechain.iroha.IrohaBlockStub

import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.ceil


class IrohaBlockEmitterTest {
    val meta = Queries.QueryPayloadMeta.newBuilder().build()
    val sig = iroha.protocol.Primitive.Signature.newBuilder().build()
    val query = Queries.BlocksQuery.newBuilder().setMeta(meta).setSignature(sig).build()

    private val file = File("resources/genesis.bin")
    val bs = file.readBytes()
    val block = IrohaBlockStub.fromProto(bs)


    val period: Long = 1000
    val unit = TimeUnit.MILLISECONDS
    val expectedBlocks = 5
    val timeout = ceil(period * expectedBlocks + period * 0.5).toLong()


    /**
     * @given a block emitter
     * @when it spawns a new block every second
     * @then in 5500 milliseconds we will accumulate 5 blocks
     */
    @Test
    fun emitterTest() {

        val blocks = mutableListOf<IrohaBlockStub>()
        var completed = false

        runBlocking {
            withTimeoutOrNull(timeout, unit, {
                async {
                    IrohaBlockEmitter(period, unit).fetchCommits(
                        query,
                        object : StreamObserver<Responses.BlockQueryResponse> {
                            override fun onNext(value: Responses.BlockQueryResponse?) {
                                val bl = value!!.blockResponse.block
                                blocks.add(IrohaBlockStub.fromProto(bl.toByteArray()))
                            }

                            override fun onCompleted() {
                            }

                            override fun onError(t: Throwable?) {
                                throw t!!
                            }
                        })
                }.await()

            })
            completed = true
        }

        assertTrue(completed)
        assertEquals(expectedBlocks, blocks.size)
        blocks.map { assertEquals(block, it) }
    }

    /**
     * @given a server that streams blocks
     * @when it sends a new block every second
     * @then in 5500 milliseconds we will accumulate 5 blocks
     */
    @Test
    fun serverTest() {

        val server = ServerBuilder.forPort(8081).addService(IrohaBlockEmitter(period, unit)).build()
        server.start()

        val channel = ManagedChannelBuilder.forAddress("localhost", 8081).usePlaintext(true).build()
        val stub = QueryServiceGrpc.newBlockingStub(channel)
        val response = stub.fetchCommits(query)

        val blocks = mutableListOf<IrohaBlockStub>()

        runBlocking {
            withTimeoutOrNull(timeout, unit, {
                async {
                    while (response.hasNext()) {
                        val bl = response.next().blockResponse.block
                        val block: IrohaBlockStub = IrohaBlockStub.fromProto(bl.toByteArray())
                        blocks.add(block)
                    }
                }.await()
            })
            channel.shutdownNow()
        }

        assertEquals(5, blocks.size)
        blocks.forEach {
            assertEquals(block, it)
        }

    }
}
