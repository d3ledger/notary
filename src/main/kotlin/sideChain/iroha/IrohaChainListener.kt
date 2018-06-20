package sideChain.iroha

import com.github.kittinunf.result.Result
import io.grpc.ManagedChannelBuilder
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import io.reactivex.schedulers.Schedulers
import mu.KLogging
import sideChain.ChainListener
import sideChain.iroha.schema.BlockService
import sideChain.iroha.schema.QueryServiceGrpc
import java.io.File
import java.util.concurrent.Executors

/**
 * Dummy implementation of [ChainListener] with effective dependencies
 */
class IrohaChainListener : ChainListener<IrohaBlockStub> {
    val meta = BlockService.QueryPayloadMeta.newBuilder().build()
    val sig = iroha.protocol.Primitive.Signature.newBuilder().build()
    val query = BlockService.BlocksQuery.newBuilder().setMeta(meta).setSignature(sig).build()

    val stub: QueryServiceGrpc.QueryServiceBlockingStub by lazy {
        val channel = ManagedChannelBuilder.forAddress("localhost", 8081).usePlaintext(true).build()
        QueryServiceGrpc.newBlockingStub(channel)
    }

    /**
     * Returns an observable that emits a new block every time it gets it from Iroha
     */
    override fun getBlockObservable(): Result<Observable<IrohaBlockStub>, Exception> {
        return Result.of {
            logger.info { "On subscribe to Iroha chain" }
            val scheduler = Schedulers.from(Executors.newSingleThreadExecutor())

            stub.fetchCommits(query).toObservable().map {
                logger.info { "New block" }
                val file = File("resources/genesis.bin")
                val bs = file.readBytes()
                IrohaBlockStub.fromProto(bs)
            }.observeOn(scheduler)
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
