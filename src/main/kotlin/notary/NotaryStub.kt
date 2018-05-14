package notary

import io.reactivex.Observable
import main.Configs
import mu.KLogging

/**
 * Dummy implementation of [Notary] with effective dependencies
 */
class NotaryStub(
    private val ethHandler: Observable<NotaryEvent>,
    private val irohaHandler: Observable<NotaryEvent>
) : Notary {

    /**
     * Handle Ehthereum event
     */
    override fun onEthEvent(ethEvent: NotaryEvent.EthChainEvent): IrohaOrderedBatch {
        logger.info { "Notary performs ETH event" }
        when (ethEvent) {
            is NotaryEvent.EthChainEvent.OnEthSidechainTransfer -> {
                logger.info { "transfer Ethereum event:" }
                logger.info { "  hash ${ethEvent.hash}" }
                logger.info { "  from ${ethEvent.from}" }
                logger.info { "  value ${ethEvent.value}" }
                logger.info { "  input ${ethEvent.input}" }

                return IrohaOrderedBatch(
                    arrayListOf(
                        IrohaTransaction(
                            Configs.irohaCreator,
                            arrayListOf(
                                IrohaCommand.CommandAddAssetQuantity(
                                    ethEvent.input,
                                    "coin#test",
                                    ethEvent.value.toString()
                                )
                            )
                        )
                    )
                )
            }
        }

        // TODO replace output with effective implementation
        return IrohaOrderedBatch(arrayListOf())
    }

    /**
     * Handle Iroha event
     */
    override fun onIrohaEvent(irohaEvent: NotaryEvent.IrohaChainEvent): IrohaOrderedBatch {
        logger.info { "Notary performs IROHA event" }

        // TODO replace output with effective implementation
        return IrohaOrderedBatch(arrayListOf())
    }

    /**
     * Relay side chain [NotaryEvent] to Iroha output
     */
    override fun irohaOutput(): Observable<IrohaOrderedBatch> {
        // TODO move business logic away from here
        return io.reactivex.Observable.merge(
            ethHandler,
            irohaHandler
        ).map { event ->
            when (event) {
                is NotaryEvent.EthChainEvent -> onEthEvent(event)
                is NotaryEvent.IrohaChainEvent -> onIrohaEvent(event)
            }
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
