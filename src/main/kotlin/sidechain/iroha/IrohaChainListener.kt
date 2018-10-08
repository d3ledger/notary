package sidechain.iroha

import com.github.kittinunf.result.Result
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import jp.co.soramitsu.iroha.ModelBlocksQueryBuilder
import model.IrohaCredential
import mu.KLogging
import sidechain.ChainListener
import sidechain.iroha.util.ModelUtil
import java.math.BigInteger
import java.util.concurrent.TimeUnit

/**
 * Dummy implementation of [ChainListener] with effective dependencies
 */
class IrohaChainListener(
    irohaHost: String,
    irohaPort: Int,
    credential: IrohaCredential
) : ChainListener<iroha.protocol.BlockOuterClass.Block> {
    val uquery = ModelBlocksQueryBuilder()
        .creatorAccountId(credential.accountId)
        .createdTime(ModelUtil.getCurrentTime())
        .queryCounter(BigInteger.valueOf(1))
        .build()

    val channel = ModelUtil.getChannel(irohaHost, irohaPort)
    val query = ModelUtil.prepareBlocksQuery(uquery, credential.keyPair)
    val stub = ModelUtil.getQueryStub(channel)

    /**
     * Returns an observable that emits a new block every time it gets it from Iroha
     */
    override fun getBlockObservable(): Result<Observable<iroha.protocol.BlockOuterClass.Block>, Exception> {
        return Result.of {
            logger.info { "On subscribe to Iroha chain" }
            stub.fetchCommits(query).toObservable().map {
                logger.info { "New Iroha block arrived. Height ${it.blockResponse.block.payload.height}" }
                it.blockResponse.block
            }
        }
    }

    /**
     * @return a block as soon as it is committed to iroha
     */
    override suspend fun getBlock(): iroha.protocol.BlockOuterClass.Block {
        return getBlockObservable().get().blockingFirst()
    }

    override fun close() {
        channel.shutdown()
        try {
            channel.awaitTermination(1, TimeUnit.SECONDS)
        } catch (ex: InterruptedException) {
            logger.warn { ex }
            channel.shutdownNow()
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
