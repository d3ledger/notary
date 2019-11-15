/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.notary

import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.SideChainEvent
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.consumer.IrohaConverter
import com.d3.commons.util.createPrettySingleThreadPool
import com.github.kittinunf.result.Result
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import jp.co.soramitsu.iroha.java.IrohaAPI
import mu.KLogging
import java.math.BigInteger
import kotlin.system.exitProcess

/**
 * Implementation of [Notary] business logic
 */

class NotaryImpl(
    private val notaryIrohaConsumer: IrohaConsumer,
    private val notaryCredential: IrohaCredential,
    private val primaryChainEvents: Observable<SideChainEvent.PrimaryBlockChainEvent>
) : Notary {

    constructor(
        notaryCredential: IrohaCredential,
        irohaAPI: IrohaAPI,
        primaryChainEvents: Observable<SideChainEvent.PrimaryBlockChainEvent>
    ) : this(
        IrohaConsumerImpl(notaryCredential, irohaAPI),
        notaryCredential,
        primaryChainEvents
    )

    /**
     * Handles primary chain deposit event. Notaries create {addAssetQuantity, transferAsset} transactions.
     */
    private fun chainAnchoredOnPrimaryChainDeposit(
        hash: String,
        time: BigInteger,
        account: String,
        asset: String,
        amount: String,
        from: String
    ): IrohaTransaction {
        logger.info { "Transfer $asset event: hash($hash) time($time) user($account) asset($asset) value ($amount)" }

        val quorum = notaryIrohaConsumer.getConsumerQuorum().get()
        return IrohaTransaction(
            notaryIrohaConsumer.creator,
            time,
            quorum,
            arrayListOf(
                IrohaCommand.CommandAddAssetQuantity(
                    asset,
                    amount
                ),
                IrohaCommand.CommandTransferAsset(
                    notaryIrohaConsumer.creator,
                    account,
                    asset,
                    from,
                    amount
                )
            )
        )
    }

    /**
     * Handles primary chain deposit event. Notaries create 'transferAsset' transactions. Without add asset qty.
     */
    private fun irohaAnchoredOnPrimaryChainDeposit(
        hash: String,
        time: BigInteger,
        account: String,
        asset: String,
        amount: String,
        from: String
    ): IrohaTransaction {
        logger.info { "Transfer Iroha anchored $asset event: hash($hash) time($time) user($account) asset($asset) value ($amount)" }
        val quorum = notaryIrohaConsumer.getConsumerQuorum().get()
        return IrohaTransaction(
            notaryIrohaConsumer.creator,
            time,
            quorum,
            arrayListOf(
                IrohaCommand.CommandTransferAsset(
                    notaryIrohaConsumer.creator,
                    account,
                    asset,
                    from,
                    amount
                )
            )
        )
    }

    /**
     * Handle primary chain event
     */
    override fun onPrimaryChainEvent(chainInputEvent: SideChainEvent.PrimaryBlockChainEvent): IrohaTransaction {
        logger.info { "Notary performs primary chain event $chainInputEvent" }

        return when (chainInputEvent) {
            is SideChainEvent.PrimaryBlockChainEvent.ChainAnchoredOnPrimaryChainDeposit -> chainAnchoredOnPrimaryChainDeposit(
                chainInputEvent.hash,
                chainInputEvent.time,
                chainInputEvent.user,
                chainInputEvent.asset,
                chainInputEvent.amount,
                chainInputEvent.from
            )

            is SideChainEvent.PrimaryBlockChainEvent.IrohaAnchoredOnPrimaryChainDeposit -> irohaAnchoredOnPrimaryChainDeposit(
                chainInputEvent.hash,
                chainInputEvent.time,
                chainInputEvent.user,
                chainInputEvent.asset,
                chainInputEvent.amount,
                chainInputEvent.from
            )
        }
    }

    /**
     * Relay side chain [SideChainEvent] to Iroha output
     */
    override fun irohaOutput(): Observable<IrohaTransaction> {
        return primaryChainEvents.map { event ->
            onPrimaryChainEvent(event)
        }
    }

    /**
     * Init Iroha consumer
     */
    override fun initIrohaConsumer(): Result<Unit, Exception> {
        logger.info { "Init Iroha consumer" }
        return Result.of {

            // Init Iroha Consumer pipeline
            irohaOutput()
                // convert from Notary model to Iroha model
                .subscribeOn(
                    Schedulers.from(
                        createPrettySingleThreadPool(
                            "notary",
                            "iroha-consumer"
                        )
                    )
                )
                .subscribe(
                    // send to Iroha network layer
                    { irohaTransaction ->
                        val tx = IrohaConverter.convert(irohaTransaction, notaryCredential.keyPair)
                        notaryIrohaConsumer.send(tx)
                            .fold(
                                { logger.info { "Send to Iroha success" } },
                                { ex -> logger.error("Send failure", ex) }
                            )
                    },
                    // on error
                    { ex ->
                        logger.error("Exit with error: ", ex)
                        exitProcess(1)
                    },
                    // should be never called
                    { logger.error { "OnComplete called" } }
                )
            Unit
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
