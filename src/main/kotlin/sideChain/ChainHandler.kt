package sideChain

/**
 * Class emit events received from side chain
 */
abstract class ChainHandler<ChainEvent> {

    /**
     * @return observable with emitted chain events
     */
    abstract fun onNewEvent(): io.reactivex.Observable<ChainEvent>
}
