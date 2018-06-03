package sideChain.iroha

import io.grpc.stub.StreamObserver
import io.reactivex.Observable
import sideChain.iroha.schema.BlockService
import sideChain.iroha.schema.QueryServiceGrpc
import java.io.File
import java.util.concurrent.TimeUnit

class IrohaBlockEmitter(val finite: Boolean = false) : QueryServiceGrpc.QueryServiceImplBase() {

    override fun fetchCommits(query: BlockService.BlocksQuery, response: StreamObserver<BlockService.BlocksQueryResponse>) {
        val file = File("resources/genesis.bin")
        val bs = file.readBytes()
        val resp = BlockService.BlockResponse.newBuilder().setBlock(iroha.protocol.BlockOuterClass.Block.parseFrom(bs))
        val blockResponse = BlockService.BlocksQueryResponse.newBuilder().setBlockResponse(resp).build()
        val observable = Observable.intervalRange(0, 5, 0, 1, TimeUnit.MILLISECONDS).map {
            response.onNext(blockResponse)
        }

        if (finite) {
            observable.blockingSubscribe()
            response.onCompleted()
        }
    }
}