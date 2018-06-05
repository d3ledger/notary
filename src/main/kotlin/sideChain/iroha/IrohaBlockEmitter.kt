package sideChain.iroha

import io.grpc.stub.StreamObserver
import io.reactivex.Observable
import sideChain.iroha.schema.BlockService
import sideChain.iroha.schema.QueryServiceGrpc
import java.io.File
import java.util.concurrent.TimeUnit

/**
 *  Simulates Iroha block streaming
 *  @param period how often it should emit new blocks
 *  @param unit the timeunit that period is associated with
 */
class IrohaBlockEmitter(val period: Long = 30, val unit: TimeUnit = TimeUnit.SECONDS) : QueryServiceGrpc.QueryServiceImplBase() {

    override fun fetchCommits(query: BlockService.BlocksQuery, response: StreamObserver<BlockService.BlocksQueryResponse>) {
        val file = File("resources/genesis.bin")
        val bs = file.readBytes()
        val resp = BlockService.BlockResponse.newBuilder().setBlock(iroha.protocol.BlockOuterClass.Block.parseFrom(bs))
        val blockResponse = BlockService.BlocksQueryResponse.newBuilder().setBlockResponse(resp).build()

        Observable.interval(period, unit).map {
            response.onNext(blockResponse)
        }.blockingSubscribe()

        response.onCompleted()
    }
}
