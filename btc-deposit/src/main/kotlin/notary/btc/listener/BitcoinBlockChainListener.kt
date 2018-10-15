package notary.btc.listener

import io.reactivex.ObservableEmitter
import mu.KLogging
import org.bitcoinj.core.Block
import org.bitcoinj.core.FilteredBlock
import org.bitcoinj.core.Peer
import org.bitcoinj.core.listeners.BlocksDownloadedEventListener
import provider.btc.address.BtcRegisteredAddressesProvider
import sidechain.SideChainEvent

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

/*
    Listener of Bitcoin blockchain events.
 */
class BitcoinBlockChainListener(
    private val btcRegisteredAddressesProvider: BtcRegisteredAddressesProvider,
    private val emitter: ObservableEmitter<SideChainEvent.PrimaryBlockChainEvent>,
    private val confidenceLevel: Int
) : BlocksDownloadedEventListener {

    override fun onBlocksDownloaded(peer: Peer?, block: Block?, filteredBlock: FilteredBlock?, blocksLeft: Int) {
        if (block == null) {
            logger.warn { "Cannot handle null BTC block" }
            return
        } else if (block.time.time < System.currentTimeMillis() - DAY_MILLIS) {
            //We cannot handle too old blocks due to Iroha time restrictions.
            return
        }
        btcRegisteredAddressesProvider.getRegisteredAddresses().fold(
            { registeredAddresses ->
                val receivedCoinsListener =
                    BitcoinTransactionListener(registeredAddresses, confidenceLevel, emitter)
                block.transactions?.forEach { tx ->
                    receivedCoinsListener.onTransaction(
                        tx,
                        block.time
                    )
                }
            },
            { ex -> logger.error("Cannot get registered BTC addresses", ex) })
    }

    /**
     * Logger
     */
    companion object : KLogging()

}

