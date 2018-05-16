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

                return IrohaOrderedBatch(
                    arrayListOf(
                        IrohaTransaction(
                            CONFIG[ConfigKeys.irohaCreator],
                            arrayListOf(
                                IrohaCommand.CommandAddAssetQuantity(
                                    ethInputEvent.input,
                                    CONFIG[ConfigKeys.irohaEthToken],
                                    ethInputEvent.value.toString()
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
