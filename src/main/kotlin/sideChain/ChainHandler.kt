package sideChain

import notary.NotaryEvent

/**
 * Class emit [NotaryEvent] received from side chain
 */
interface ChainHandler {

    /**
     * @return observable with emitted chain events
     */
    fun onNewEvent(): io.reactivex.Observable<NotaryEvent>
}
