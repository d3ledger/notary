package notary


import io.grpc.ManagedChannelBuilder
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.experimental.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import sideChain.iroha.IrohaBlockEmitter
import sideChain.iroha.IrohaBlockStub
import sideChain.iroha.schema.BlockService
import sideChain.iroha.schema.QueryServiceGrpc
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.ceil


class IrohaBlockEmitterTest {
    val meta = BlockService.QueryPayloadMeta.newBuilder().build()
    val sig = iroha.protocol.Primitive.Signature.newBuilder().build()
    val query = BlockService.BlocksQuery.newBuilder().setMeta(meta).setSignature(sig).build()

    private val file = File("resources/genesis.bin")
    val bs = file.readBytes()
    val block = IrohaBlockStub.fromProto(bs)


    val period: Long = 1
    val unit = TimeUnit.SECONDS
    val expectedBlocks = 5
    val timeout = ceil(period * expectedBlocks * 1.2).toLong()


    @Test
    fun emitterTest() {

        val blocks = mutableListOf<IrohaBlockStub>()
        var completed = false

        runBlocking {
            withTimeoutOrNull(timeout, unit, {
                async {
                    IrohaBlockEmitter(period, unit).fetchCommits(query, object : StreamObserver<BlockService.BlocksQueryResponse> {
                        override fun onNext(value: BlockService.BlocksQueryResponse?) {
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