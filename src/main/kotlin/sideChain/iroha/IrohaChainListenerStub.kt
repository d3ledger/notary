package sideChain.iroha

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
class IrohaChainListenerStub : ChainListener<IrohaBlockStub> {

    /**
     * TODO 20:17, @muratovv: rework with effective impelementation
     * current implementation emit new mock eth block each 3 second
     */
    override fun onNewBlockObservable(): Observable<IrohaBlockStub> {
        logger.info { "On subscribe to Iroha chain" }
        val scheduler = Schedulers.from(Executors.newSingleThreadExecutor())
        return Observable.interval(3, TimeUnit.SECONDS).map {
            logger.info { "Timestamp = $it" }
            mock<IrohaBlockStub> {
            }
        }.observeOn(scheduler).subscribeOn(scheduler)
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
