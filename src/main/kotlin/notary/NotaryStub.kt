package notary

import com.nhaarman.mockito_kotlin.mock
import io.reactivex.Observable
import main.Configs
import mu.KLogging
import sideChain.*

/**
 * Dummy implementation of [Notary] with effective dependencies
 */
class NotaryStub(
    private val ethHandler: ChainHandler,
    private val irohaHandler: ChainHandler
) : Notary {

    override fun onEthEvent(ethEvent: NotaryEvent.EthChainEvent) {
        logger.info { "Notary performs ETH event" }
    }

    override fun onIrohaEvent(irohaEvent: NotaryEvent.IrohaChainEvent) {
        logger.info { "Notary performs IROHA event" }
    }

    /**
     * Dummy implementation with relaying eth events for output
     */
    override fun irohaOutput(): Observable<IrohaOrderedBatch> {
        return io.reactivex.Observable.merge(
            ethHandler.onNewEvent(),
            irohaHandler.onNewEvent()
        ).map {
            when (it) {
                is NotaryEvent.EthChainEvent -> onEthEvent(mock<NotaryEvent.EthChainEvent.OnEthSidechainTransfer>())
                is NotaryEvent.IrohaChainEvent -> onIrohaEvent(mock<NotaryEvent.IrohaChainEvent.OnIrohaAddPeer>())
            }
            logger.info { "Notary does some work" }

            // emit event to Iroha
            val batch = IrohaOrderedBatch()

            val command1 = arrayOf(
                IrohaCommand.CommandAddAssetQuantity(
                    Configs.irohaCreator,
                    "coin#test",
                    "1.00"
                ),
                IrohaCommand.CommandAddSignatory(
                    Configs.irohaCreator,
                    "pubkey12345678901234567890123456"
                ),
                IrohaCommand.CommandCreateAsset(
                    "coin2",
                    "test",
                    2
                )
            )
            val command2 = arrayOf(
                IrohaCommand.CommandSetAccountDetail(
                    Configs.irohaCreator,
                    "key",
                    "val"
                ),
                IrohaCommand.CommandTransferAsset(
                    Configs.irohaCreator,
                    "user@test",
                    "coin#test",
                    "descr",
                    "1.00"
                )
            )

            batch.addTransaction(command1)
            batch.addTransaction(command2)

            batch
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
