package algorithm

import mu.KLogging
import sideChain.IrohaOrderedBatch

/**
 * Dummy implementation of [IrohaConsumer] with effective dependency
 */
class IrohaConsumerStub(private val notary: Notary) : IrohaConsumer {

    init {
        notary.irohaOutput().subscribe {
            onIrohaEvent(it)
        }
    }

    /**
     * Provides dummy output to log just for verification
     */
    override fun onIrohaEvent(batch: IrohaOrderedBatch) {
        logger.info { "TX to IROHA" }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
