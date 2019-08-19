/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.notary

import com.d3.commons.sidechain.SideChainEvent
import com.github.kittinunf.result.Result

/**
 * Interface for performing 2WP in Iroha and side chains
 */
interface Notary {

    /**
     * Called when primary chain event is occurred
     * @return transaction to be committed to Iroha
     */
    fun onPrimaryChainEvent(chainInputEvent: SideChainEvent.PrimaryBlockChainEvent): IrohaTransaction

    /**
     * Observable with output for sending to Iroha
     */
    fun irohaOutput(): io.reactivex.Observable<IrohaTransaction>

    /**
     * Init Iroha consumer
     */
    fun initIrohaConsumer(): Result<Unit, Exception>
}
