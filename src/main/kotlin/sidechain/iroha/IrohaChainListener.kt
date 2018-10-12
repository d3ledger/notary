package sidechain.iroha

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import io.reactivex.Observable
import model.IrohaCredential
import mu.KLogging
import sidechain.ChainListener
import sidechain.iroha.consumer.IrohaNetworkImpl

/**
 * Dummy implementation of [ChainListener] with effective dependencies
 */
class IrohaChainListener(
    irohaHost: String,
    irohaPort: Int,
    val credential: IrohaCredential
) : ChainListener<iroha.protocol.BlockOuterClass.Block> {
    val irohaNetwork = IrohaNetworkImpl(irohaHost, irohaPort)

    /**
     * Returns an observable that emits a new block every time it gets it from Iroha
     */
    override fun getBlockObservable(): Result<Observable<iroha.protocol.BlockOuterClass.Block>, Exception> {
        logger.info { "On subscribe to Iroha chain" }
        return irohaNetwork.getBlocksStreaming(credential).map {
            it.map {
                logger.info { "New Iroha block arrived. Height ${it.blockResponse.block.payload.height}" }
                it.blockResponse.block
            }
        }
    }

    /**
     * @return a block as soon as it is committed to iroha
     */
    override suspend fun getBlock(): iroha.protocol.BlockOuterClass.Block {
        return getBlockObservable().get().blockingFirst()
    }

    override fun close() {
        irohaNetwork.close()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
