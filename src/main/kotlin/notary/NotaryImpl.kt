package notary

import com.github.kittinunf.result.Result
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import mu.KLogging
import sidechain.SideChainEvent
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaConverterImpl
import java.util.concurrent.Executors

/**
 * Implementation of [Notary] business logic
 */
class NotaryImpl(
    private val notaryConfig: NotaryConfig,
    private val primaryChainEvents: Observable<SideChainEvent.PrimaryBlockChainEvent>,
    private val domain: String
) : Notary {

    /** Notary account in Iroha */
    private val creator = notaryConfig.iroha.creator

    /**
     * Handles primary chain deposit event. Notaries create the ordered bunch of
     * transactions:{tx1: setAccountDetail, tx2: CreateAsset, tx3: addAssetQuantity, transferAsset}.
     * SetAccountDetail insert into notary account information about the transaction (hash) for rollback.
     */
    private fun onPrimaryChainDeposit(
        hash: String,
        account: String,
        asset: String,
        amount: String,
        from: String
    ): IrohaOrderedBatch {

        logger.info { "transfer $asset event: hash($hash) user($account) asset($asset) value ($amount)" }

        return IrohaOrderedBatch(
            arrayListOf(
                IrohaTransaction(
                    creator,
                    arrayListOf(
                        // insert into Iroha account information for rollback
                        IrohaCommand.CommandSetAccountDetail(
                            creator,
                            "last_tx",
                            hash
                        )
                    )
                ),
                IrohaTransaction(
                    creator,
                    arrayListOf(
                        IrohaCommand.CommandCreateAsset(
                            asset,
                            domain,
                            0
                        )
                    )
                ),
                IrohaTransaction(
                    creator,
                    arrayListOf(
                        IrohaCommand.CommandAddAssetQuantity(
                            "$asset#$domain",
                            amount
                        ),
                        IrohaCommand.CommandTransferAsset(
                            creator,
                            account,
                            "$asset#$domain",
                            from,
                            amount
                        )
                    )
                )
            )
        )
    }

    /**
     * Handle primary chain event
     */
    override fun onPrimaryChainEvent(chainInputEvent: SideChainEvent.PrimaryBlockChainEvent): IrohaOrderedBatch {
        logger.info { "Notary performs primary chain event" }
        return when (chainInputEvent) {
            is SideChainEvent.PrimaryBlockChainEvent.OnPrimaryChainDeposit -> onPrimaryChainDeposit(
                chainInputEvent.hash,
                chainInputEvent.user,
                chainInputEvent.asset,
                chainInputEvent.amount.toString(),
                chainInputEvent.from
            )
        }
    }

    /**
     * Relay side chain [SideChainEvent] to Iroha output
     */
    override fun irohaOutput(): Observable<IrohaOrderedBatch> {
        return primaryChainEvents.map { event ->
            onPrimaryChainEvent(event)
        }
    }

    override fun initIrohaConsumer(): Result<Unit, Exception> {
        logger.info { "Init Iroha consumer" }
        return Result.of {
            val irohaConsumer = IrohaConsumerImpl(notaryConfig.iroha)
            // Init Iroha Consumer pipeline
            irohaOutput()
                // convert from Notary model to Iroha model
                // TODO rework Iroha batch transaction
                .flatMapIterable { batch -> IrohaConverterImpl().convert(batch) }
                .subscribeOn(Schedulers.from(Executors.newSingleThreadExecutor()))
                .subscribe(
                    // send to Iroha network layer
                    { unsignedTx ->
                        irohaConsumer.sendAndCheck(unsignedTx)
                            .fold(
                                { logger.info { "send to Iroha success" } },
                                { ex ->
                                    logger.error { "send failure $ex" }
                                }
                            )
                    },
                    // on error
                    { ex -> notary.btc.BtcNotaryInitialization.Companion.logger.error { "OnError called $ex" } },
                    // should be never called
                    { error { "OnComplete called" } }
                )
            Unit
        }
    }


    /**
     * Logger
     */
    companion object : KLogging()
}


