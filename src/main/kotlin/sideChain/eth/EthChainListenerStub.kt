package sideChain.eth

import com.nhaarman.mockito_kotlin.mock
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import mu.KLogging
import sideChain.ChainListener
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Dummy implementation of [ChainListener] with effective dependencies
 */
class EthChainListenerStub : ChainListener<EthBlockStub> {

    /**
     * // TODO 20:17, @muratovv: rework with effective impelementation
     * // current implementation emit new mock eth block each 4 second
     */
    override fun onNewBlockObservable(): Observable<EthBlockStub> {
        logger.info { "On subscribe to ETH chain" }
        val scheduler = Schedulers.from(Executors.newSingleThreadExecutor())
        return Observable.interval(4, TimeUnit.SECONDS).map {
            logger.info { "Timestamp = $it" }
            mock<EthBlockStub> {
            }
        }.observeOn(scheduler).subscribeOn(scheduler)
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
