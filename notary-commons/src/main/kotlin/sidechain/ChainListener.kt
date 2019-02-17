package sidechain

import com.github.kittinunf.result.Result
import java.io.Closeable

/**
 * Class listens for new [Block] in side block chain
 */
interface ChainListener<Block> : Closeable {

    /**
     * @return Observable on blocks that committed to the network
     */
    fun getBlockObservable(autoAck : Boolean = true): Result<io.reactivex.Observable<Block>, Exception>

    /**
     * @return a block that was committed into network
     */
    suspend fun getBlock(autoAck : Boolean = true): Block

}
