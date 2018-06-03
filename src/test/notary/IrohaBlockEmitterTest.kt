package notary


import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import sideChain.iroha.IrohaBlockEmitter
import sideChain.iroha.IrohaBlockStub
import sideChain.iroha.schema.BlockService
import java.io.File

class IrohaBlockEmitterTest {
    @Test
    fun emitterTest() {
        val meta = BlockService.QueryPayloadMeta.newBuilder().build()
        val sig = iroha.protocol.Primitive.Signature.newBuilder().build()
        val query = BlockService.BlocksQuery.newBuilder().setMeta(meta).setSignature(sig).build()
        val blocks = mutableListOf<IrohaBlockStub>()
        var completed = false

        IrohaBlockEmitter(true).fetchCommits(query, object : StreamObserver<BlockService.BlocksQueryResponse> {
            override fun onNext(value: BlockService.BlocksQueryResponse?) {
                val bl = value!!.blockResponse.block
                blocks.add(IrohaBlockStub.fromProto(bl.toByteArray()))
            }

            override fun onCompleted() {
                completed = true
            }

            override fun onError(t: Throwable?) {
                throw t!!
            }
        })

        val file = File("resources/genesis.bin")
        val bs = file.readBytes()
        val block = IrohaBlockStub.fromProto(bs)

        assertTrue(completed)
        assertEquals(5, blocks.size)
        blocks.map { assertEquals(block, it) }
    }
}