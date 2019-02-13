package withdrawal.btc.listener

import fee.BtcFeeRateService
import fee.FeeRate
import fee.avgFeeRate
import mu.KLogging
import org.bitcoinj.core.Block
import org.bitcoinj.core.FilteredBlock
import org.bitcoinj.core.Peer
import org.bitcoinj.core.listeners.BlocksDownloadedEventListener
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
private const val INIT_TIME_FEE_RATE_UPDATE_DELAY_MIN = 1
private const val FEE_RATE_UPDATE_DELAY_MIN = 5

/**
 * This listener listens to fee rate changes
 */
class BitcoinBlockChainFeeRateListener(
    private val btcFeeRateService: BtcFeeRateService
) : BlocksDownloadedEventListener, Closeable {

    /**
     * Executor that periodically updates fee rate using Iroha
     */
    private val feeRateSetterExecutorService = Executors.newSingleThreadScheduledExecutor()

    /**
     * Last fee rate.
     * We need @Volatile, because this value is written from one thread and read from another.
     */
    @Volatile
    private var lastFeeRate = FeeRate()

    init {
        /**
         * We cannot update fee rate every time new block arrives,
         * because this listener may be called too often (bitcoinj downloads all blocks since last stoppage).
         * This is why we use another thread to set new fee rate every [FEE_RATE_UPDATE_DELAY_MIN] minutes.
         */
        feeRateSetterExecutorService.scheduleWithFixedDelay(
            {
                val feeRate = lastFeeRate
                if (!feeRate.isSet()) {
                    // We don't update fee rate if it was not set
                    return@scheduleWithFixedDelay
                }
                // Setting fee rate
                btcFeeRateService.setFeeRate(feeRate)
                    .fold(
                        { logger.info { "Fee rate was set to ${feeRate.avgFeeRate}" } },
                        { ex -> logger.error("Cannot update fee rate", ex) })
            },
            INIT_TIME_FEE_RATE_UPDATE_DELAY_MIN.toLong(),
            FEE_RATE_UPDATE_DELAY_MIN.toLong(),
            TimeUnit.MINUTES
        )
        logger.info { "Fee rate updating thread was started" }
    }

    override fun onBlocksDownloaded(peer: Peer?, block: Block, filteredBlock: FilteredBlock?, blocksLeft: Int) {
        //We are not interested in old blocks
        if (block.time.time < System.currentTimeMillis() - DAY_MILLIS) {
            return
        }
        lastFeeRate = FeeRate(block.avgFeeRate(), block.timeSeconds)
        logger.info { "Last fee rate is $lastFeeRate" }
    }

    override fun close() {
        feeRateSetterExecutorService.shutdownNow()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
