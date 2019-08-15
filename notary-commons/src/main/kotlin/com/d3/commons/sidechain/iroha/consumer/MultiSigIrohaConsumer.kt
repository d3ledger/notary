/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.iroha.consumer

import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.consumer.status.IrohaTxStatus
import iroha.protocol.Endpoint
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.detail.InlineTransactionStatusObserver
import jp.co.soramitsu.iroha.java.subscription.WaitForTerminalStatus
import java.util.concurrent.atomic.AtomicReference

/**
 * Statuses that we consider terminal
 */
val mstTerminalStatuses = terminalStatuses.plus(Endpoint.TxStatus.MST_PENDING)

/**
 * Endpoint of Iroha to write transactions.
 * Quite similar to [IrohaConsumerImpl] but considers MST_PENDING status as terminal(won't wait MST until it fully committed).
 * @param irohaCredential - for creating transactions
 * @param irohaAPI - Iroha network
 */
class MultiSigIrohaConsumer(irohaCredential: IrohaCredential, irohaAPI: IrohaAPI) :
    IrohaConsumerImpl(irohaCredential, irohaAPI) {

    override val waitForTerminalStatus = WaitForTerminalStatus(mstTerminalStatuses)

    /**
     * Create tx status observer.
     * It does the same thing as [IrohaConsumerImpl], but also reacts on MST_PENDING status
     * @param statusReference - reference to an object that will hold tx status after observer completion
     * @return tx status observer
     */
    override protected fun getTxStatusObserver(statusReference: AtomicReference<IrohaTxStatus>):
            InlineTransactionStatusObserver.InlineTransactionStatusObserverBuilder {
        return super.getTxStatusObserver(statusReference)
            .onMstPending { statusReference.set(IrohaTxStatus.createSuccessful(Endpoint.TxStatus.MST_PENDING)) }
    }
}
