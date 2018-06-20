package notary

import io.reactivex.Observable
import main.ConfigKeys
import mu.KLogging
import java.math.BigInteger

/**
 * Implementation of [Notary] business logic
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
     * Handle Ethereum deposit event. Notaries create the ordered bunch of
     * transactions:{tx1: setAccountDetail, tx2: CreateAsset, tx3: addAssetQuantity, transferAsset}.
     * SetAccountDetail insert into notary account information about the transaction (hash) for rollback.
     */
    private fun onEthSidechainDeposit(
        hash: String,
        account: String,
        asset: String,
        amount: BigInteger
    ): IrohaOrderedBatch {
        val domain = "ethereum"

        logger.info { "transfer Ethereum event: hash($hash) user($account) asset($asset) value ($amount)" }

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
                            creator,
                            "$asset#$domain",
                            amount.toString()
                        ),
                        IrohaCommand.CommandTransferAsset(
                            creator,
                            account,
                            "$asset#$domain",
                            "",
                            amount.toString()
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
        return when (ethInputEvent) {
            is NotaryInputEvent.EthChainInputEvent.OnEthSidechainDeposit -> onEthSidechainDeposit(
                ethInputEvent.hash,
                ethInputEvent.user,
                ethereumAssetId,
                ethInputEvent.amount
            )
            is NotaryInputEvent.EthChainInputEvent.OnEthSidechainDepositToken -> onEthSidechainDeposit(
                ethInputEvent.hash,
                ethInputEvent.user,
                ethInputEvent.token,
                ethInputEvent.amount
            )
        }
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
        return Observable.merge(
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
