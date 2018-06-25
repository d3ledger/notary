package sidechain.iroha

import com.github.kittinunf.result.Result
import io.grpc.ManagedChannelBuilder
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import io.reactivex.schedulers.Schedulers
import jp.co.soramitsu.iroha.ModelBlocksQueryBuilder
import mu.KLogging
import sidechain.ChainListener
import sidechain.iroha.util.getCurrentTime
import sidechain.iroha.util.getKeys
import sidechain.iroha.util.getQueryStub
import sidechain.iroha.util.prepareBlocksQuery
import java.io.File
import java.math.BigInteger
import java.util.concurrent.Executors

/**
 * Dummy implementation of [ChainListener] with effective dependencies
 */
class IrohaChainListener : ChainListener<IrohaBlockStub> {
    val admin = "admin@notary"
    val keypair = getKeys("deploy/iroha/keys", admin)
    val uquery = ModelBlocksQueryBuilder()
        .creatorAccountId(admin)
        .createdTime(getCurrentTime())
        .queryCounter(BigInteger.valueOf(1))
        .build()

    val query = prepareBlocksQuery(uquery, keypair)
    val stub = getQueryStub()

    /**
     * Returns an observable that emits a new block every time it gets it from Iroha
     */
    override fun getBlockObservable(): Result<Observable<IrohaBlockStub>, Exception> {
        return Result.of {
            logger.info { "On subscribe to Iroha chain" }
            val scheduler = Schedulers.from(Executors.newSingleThreadExecutor())

            stub.fetchCommits(query).toObservable().map {
                IrohaBlockStub.fromProto(it.blockResponse.toByteArray())
            }.observeOn(scheduler)
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
