package sidechain.iroha

import com.github.kittinunf.result.Result
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import io.reactivex.schedulers.Schedulers
import jp.co.soramitsu.iroha.ModelBlocksQueryBuilder
import main.ConfigKeys
import mu.KLogging
import notary.CONFIG
import sidechain.ChainListener
import sidechain.iroha.util.ModelUtil
import java.math.BigInteger
import java.util.concurrent.Executors

/**
 * Dummy implementation of [ChainListener] with effective dependencies
 */
class IrohaChainListener : ChainListener<iroha.protocol.BlockOuterClass.Block> {
    val admin = CONFIG[ConfigKeys.notaryIrohaAccount]
    val keypair = ModelUtil.loadKeypair(
        CONFIG[ConfigKeys.notaryPubkeyPath],
        CONFIG[ConfigKeys.notaryPrivkeyPath]
    ).get()

    val uquery = ModelBlocksQueryBuilder()
        .creatorAccountId(admin)
        .createdTime(ModelUtil.getCurrentTime())
        .queryCounter(BigInteger.valueOf(1))
        .build()

    val query = ModelUtil.prepareBlocksQuery(uquery, keypair)
    val stub = ModelUtil.getQueryStub()

    /**
     * Returns an observable that emits a new block every time it gets it from Iroha
     */
    override fun getBlockObservable(): Result<Observable<iroha.protocol.BlockOuterClass.Block>, Exception> {
        return Result.of {
            logger.info { "On subscribe to Iroha chain" }
            val scheduler = Schedulers.from(Executors.newSingleThreadExecutor())

            stub.fetchCommits(query).toObservable().map {
                logger.info { "New Iroha block arrived" }
                //TODO x3medima17 02.07.2018, return business model object instead of proto
                it.blockResponse.block
            }.observeOn(scheduler)
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
