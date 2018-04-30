package sideChain

/**
 * Class listens for new [Block] in side block chain
 */
interface ChainListener<Block> {

    /**
     * @return blocks that committed in the network
     */
    fun onNewBlockObservable(): io.reactivex.Observable<Block>
}
