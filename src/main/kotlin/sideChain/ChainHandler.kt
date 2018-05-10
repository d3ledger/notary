package sideChain

import notary.NotaryEvent

/**
 * Class extracts [NotaryEvent] received from side chain blocks
 */
interface ChainHandler<Block> {

    /**
     * Parse block and find interesting transactions.
     * @return observable with emitted chain events
     */
    fun parseBlock(block: Block): List<NotaryEvent>
}
