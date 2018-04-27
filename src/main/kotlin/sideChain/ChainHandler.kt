package sideChain

/**
 * Class emit events received from side chain
 */
interface ChainHandler<ChainEvent> {

    /**
     * @return observable with emitted chain events
     */
    fun onNewEvent(): io.reactivex.Observable<ChainEvent>
}
