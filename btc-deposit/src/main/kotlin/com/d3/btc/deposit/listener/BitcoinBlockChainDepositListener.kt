package com.d3.btc.deposit.listener

import com.d3.btc.provider.BtcRegisteredAddressesProvider
import io.reactivex.ObservableEmitter
import mu.KLogging
import org.bitcoinj.core.Block
import org.bitcoinj.core.FilteredBlock
import org.bitcoinj.core.Peer
import org.bitcoinj.core.listeners.BlocksDownloadedEventListener
import sidechain.SideChainEvent
import java.util.concurrent.ExecutorService

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

/*
    Listener of Bitcoin blockchain events.
 */
class BitcoinBlockChainDepositListener(
    private val btcRegisteredAddressesProvider: BtcRegisteredAddressesProvider,
    private val emitter: ObservableEmitter<SideChainEvent.PrimaryBlockChainEvent>,
    private val confidenceListenerExecutorService: ExecutorService,
    private val confidenceLevel: Int
) : BlocksDownloadedEventListener {

    private val processedBlocks = HashSet<String>()

    override fun onBlocksDownloaded(peer: Peer?, block: Block, filteredBlock: FilteredBlock?, blocksLeft: Int) {
        if (block.time.time < System.currentTimeMillis() - DAY_MILLIS) {
            //We cannot handle too old blocks due to Iroha time restrictions.
            return
        } else if (processedBlocks.contains(block.hashAsString)) {
            /*
            Sometimes Bitcoin blockchain misbehaves. It can see duplicated blocks.
            Simple workaround - store previously seen blocks.
            */
            //TODO remove this check after Iroha "replay attack" fix
            logger.warn { "Block ${block.hashAsString} has been already processed" }
            return
        }
        btcRegisteredAddressesProvider.getRegisteredAddresses().fold(
            { registeredAddresses ->
                processedBlocks.add(block.hashAsString)
                val receivedCoinsListener =
                    BitcoinTransactionListener(
                        registeredAddresses,
                        confidenceLevel,
                        emitter,
                        confidenceListenerExecutorService
                    )
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
