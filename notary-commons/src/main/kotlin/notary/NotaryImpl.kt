package notary

import com.github.kittinunf.result.Result
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import jp.co.soramitsu.iroha.java.IrohaAPI
import model.IrohaCredential
import mu.KLogging
import provider.NotaryPeerListProvider
import sidechain.SideChainEvent
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaConverter
import java.math.BigInteger
import java.util.concurrent.Executors

/**
 * Implementation of [Notary] business logic
 */
class NotaryImpl(
    private val notaryCredential: IrohaCredential,
    private val irohaAPI: IrohaAPI,
    private val primaryChainEvents: Observable<SideChainEvent.PrimaryBlockChainEvent>,
    private val domain: String,
    private val peerListProvider: NotaryPeerListProvider
) : Notary {


    /** Notary account in Iroha */
    private val creator = notaryCredential.accountId

    /**
     * Handles primary chain deposit event. Notaries create the ordered bunch of
     * transactions: {tx1: setAccountDetail, tx2: addAssetQuantity, transferAsset}.
     * SetAccountDetail insert into notary account information about the transaction (hash) for rollback.
     */
    private fun onPrimaryChainDeposit(
        hash: String,
        time: BigInteger,
        account: String,
        asset: String,
        amount: String,
        from: String
    ): IrohaAtomicBatch {

        logger.info { "Transfer $asset event: hash($hash) time($time) user($account) asset($asset) value ($amount)" }

        val quorum = peerListProvider.getPeerList().size

        return IrohaAtomicBatch(
            arrayListOf(
                IrohaTransaction(
                    creator,
                    time,
                    quorum,
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
                    time,
                    quorum,
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
    override fun onPrimaryChainEvent(chainInputEvent: SideChainEvent.PrimaryBlockChainEvent): IrohaAtomicBatch {
        logger.info { "Notary performs primary chain event $chainInputEvent" }
        return when (chainInputEvent) {
            is SideChainEvent.PrimaryBlockChainEvent.OnPrimaryChainDeposit -> onPrimaryChainDeposit(
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
    override fun irohaOutput(): Observable<IrohaAtomicBatch> {
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
            val irohaConsumer = IrohaConsumerImpl(notaryCredential, irohaAPI)

            // Init Iroha Consumer pipeline
            irohaOutput()
                // convert from Notary model to Iroha model
                .subscribeOn(Schedulers.from(Executors.newSingleThreadExecutor()))
                .subscribe(
                    // send to Iroha network layer
                    { batch ->
                        val lst = IrohaConverter.convert(batch, notaryCredential.keyPair)
                        irohaConsumer.send(lst)
                            .fold(
                                { logger.info { "Send to Iroha success" } },
                                { ex -> logger.error("Send failure", ex) }
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
