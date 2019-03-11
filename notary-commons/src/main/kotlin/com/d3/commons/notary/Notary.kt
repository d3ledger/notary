package com.d3.commons.notary

import com.github.kittinunf.result.Result
import com.d3.commons.sidechain.SideChainEvent

/**
 * Interface for performing 2WP in Iroha and side chains
 */
interface Notary {

    /**
     * Called when primary chain event is occurred
     * @return transaction to be commited to Iroha
     */
    fun onPrimaryChainEvent(chainInputEvent: SideChainEvent.PrimaryBlockChainEvent): IrohaOrderedBatch

    /**
     * Observable with output for sending to Iroha
     */
    fun irohaOutput(): io.reactivex.Observable<IrohaOrderedBatch>

    /**
     * Init Iroha consumer
     */
    fun initIrohaConsumer(): Result<Unit, Exception>
}
