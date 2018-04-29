package sideChain

/**
 * Class listens for new blocks in block chain
 * @param Block type of blocks in block chain
 */
interface ChainListener<Block> {

    /**
     * @return blocks that committed in the network
     */
    fun onNewBlockObservable(): io.reactivex.Observable<Block>
}
