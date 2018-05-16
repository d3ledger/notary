package notary

import io.reactivex.Observable
import main.CONFIG
import main.ConfigKeys
import mu.KLogging

/**
 * Dummy implementation of [Notary] with effective dependencies
 */
class NotaryImpl(
    private val ethHandler: Observable<NotaryInputEvent>,
    private val irohaHandler: Observable<NotaryInputEvent>
) : Notary {

    /** Notary account in Iroha */
    val creator = CONFIG[ConfigKeys.irohaCreator]

    /** Ethereum asset id in Iroha */
    val ethereumAssetId = CONFIG[ConfigKeys.irohaEthToken]

    /**
     * Handle Ethereum deposite event. Notaries create the ordered bunch of
     * transactions:{tx1: setAccountDetail, tx2: addAssetQuantity, tx3: transferAsset}.
     * SetAccountDetail insert into notary account information about the transaction (hash) for rollback.
     */
    private fun onEthSidechainDeposit(ethInputEvent: NotaryInputEvent.EthChainInputEvent.OnEthSidechainDeposit): IrohaOrderedBatch {
        val destAccountId = ethInputEvent.input

        return IrohaOrderedBatch(
            arrayListOf(
                IrohaTransaction(
                    creator,
                    arrayListOf(
                        IrohaCommand.CommandSetAccountDetail(
                            creator,
                            // add Ethereum tx hash as a key
                            ethInputEvent.hash,
                            // set Ethereum tx sender as a value
                            ethInputEvent.from
                        )
                    )
                ),
                IrohaTransaction(
                    creator,
                    arrayListOf(
                        IrohaCommand.CommandAddAssetQuantity(
                            creator,
                            ethereumAssetId,
                            ethInputEvent.value.toString()
                        )
                    )
                ),
                IrohaTransaction(
                    creator,
                    arrayListOf(
                        IrohaCommand.CommandTransferAsset(
                            creator,
                            destAccountId,
                            ethereumAssetId,
                            ethInputEvent.hash,
                            ethInputEvent.value.toString()
                        )
                    )
                )
            )
        )
    }

    /**
     * Handle Ehthereum event
     */
    override fun onEthEvent(ethInputEvent: NotaryInputEvent.EthChainInputEvent): IrohaOrderedBatch {
        logger.info { "Notary performs ETH event" }
        when (ethInputEvent) {
            is NotaryInputEvent.EthChainInputEvent.OnEthSidechainDeposit -> {
                logger.info { "transfer Ethereum event:" }
                logger.info { "  hash ${ethInputEvent.hash}" }
                logger.info { "  from ${ethInputEvent.from}" }
                logger.info { "  value ${ethInputEvent.value}" }
                logger.info { "  input ${ethInputEvent.input}" }

                return onEthSidechainDeposit(ethInputEvent)
            }
        }

        // TODO replace output with effective implementation
        return IrohaOrderedBatch(arrayListOf())
    }

    /**
     * Handle Iroha event
     */
    override fun onIrohaEvent(irohaInputEvent: NotaryInputEvent.IrohaChainInputEvent): IrohaOrderedBatch {
        logger.info { "Notary performs IROHA event" }

        // TODO replace output with effective implementation
        return IrohaOrderedBatch(arrayListOf())
    }

    /**
     * Relay side chain [NotaryInputEvent] to Iroha output
     */
    override fun irohaOutput(): Observable<IrohaOrderedBatch> {
        // TODO move business logic away from here
        return io.reactivex.Observable.merge(
            ethHandler,
            irohaHandler
        ).map { event ->
            when (event) {
                is NotaryInputEvent.EthChainInputEvent -> onEthEvent(event)
                is NotaryInputEvent.IrohaChainInputEvent -> onIrohaEvent(event)
            }
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
