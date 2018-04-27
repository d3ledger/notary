package sideChain

/**
 * Class listen new blocks in block chain
 * @param Block type of blocks in block chain
 */
abstract class ChainListener<Block> {

    /**
     * @return blocks that committed in the network
     */
    abstract fun onNewBlock(): io.reactivex.Observable<Block>
}
