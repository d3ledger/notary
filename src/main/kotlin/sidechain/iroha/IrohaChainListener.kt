package sidechain.iroha

import com.github.kittinunf.result.Result
import io.grpc.ManagedChannelBuilder
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import io.reactivex.schedulers.Schedulers
import iroha.protocol.Queries.BlocksQuery
import iroha.protocol.Queries.QueryPayloadMeta
import iroha.protocol.QueryServiceGrpc
import mu.KLogging
import sidechain.ChainListener
import java.io.File
import java.util.concurrent.Executors


/**
 * Dummy implementation of [ChainListener] with effective dependencies
 */
class IrohaChainListener : ChainListener<IrohaBlockStub> {
    val meta = QueryPayloadMeta.newBuilder().build()
    val sig = iroha.protocol.Primitive.Signature.newBuilder().build()
    val query = BlocksQuery.newBuilder().setMeta(meta).setSignature(sig).build()

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
            }.subscribeOn(scheduler)
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
