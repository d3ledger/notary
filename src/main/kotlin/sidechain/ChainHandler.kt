package sidechain


/**
 * Class extracts [SideChainEvent] received from side chain blocks
 */
interface ChainHandler<Block> {

    /**
     * Parse block and find interesting transactions.
     * @return observable with emitted chain events
     */
    fun parseBlock(block: Block): List<SideChainEvent>
}
