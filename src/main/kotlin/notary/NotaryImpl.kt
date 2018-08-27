package notary

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import config.IrohaConfig
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import mu.KLogging
import sidechain.SideChainEvent
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaConverterImpl
import sidechain.iroha.util.ModelUtil

/**
 * Implementation of [Notary] business logic
 */
class NotaryImpl(
    private val irohaConfig: IrohaConfig,
    private val primaryChainEvents: Observable<SideChainEvent.PrimaryBlockChainEvent>,
    private val domain: String
) : Notary {

    /** Notary account in Iroha */
    private val creator = irohaConfig.creator

    /**
     * Handles primary chain deposit event. Notaries create the ordered bunch of
     * transactions: {tx1: setAccountDetail, tx2: addAssetQuantity, transferAsset}.
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

    /**
     * Init Iroha consumer
     */
    override fun initIrohaConsumer(): Result<Unit, Exception> {
        logger.info { "Init Iroha consumer" }
        return ModelUtil.loadKeypair(irohaConfig.pubkeyPath, irohaConfig.privkeyPath)
            .map { keyPair ->
                val irohaConsumer = IrohaConsumerImpl(irohaConfig)

                // Init Iroha Consumer pipeline
                irohaOutput()
                    // convert from Notary model to Iroha model
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                        // send to Iroha network layer
                        { batch ->
                            val lst = IrohaConverterImpl().convert(batch)
                            irohaConsumer.sendAndCheck(lst)
                                .fold(
                                    { logger.info { "send to Iroha success" } },
                                    { ex -> logger.error("send failure", ex) }
                                )
                        },
                        // on error
                        { ex -> logger.error("OnError called", ex) },
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
