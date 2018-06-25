package sidechain

import com.github.kittinunf.result.Result

/**
 * Class listens for new [Block] in side block chain
 */
interface ChainListener<Block> {

    /**
     * @return Observable on blocks that committed to the network
     */
    fun getBlockObservable(): Result<io.reactivex.Observable<Block>, Exception>

}
