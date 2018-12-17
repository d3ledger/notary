package sidechain.iroha

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import io.reactivex.Observable
import jp.co.soramitsu.iroha.java.IrohaAPI
import model.IrohaCredential
import mu.KLogging
import sidechain.ChainListener
import sidechain.iroha.util.ModelUtil

/**
 * Dummy implementation of [ChainListener] with effective dependencies
 */
class IrohaChainListener(
    irohaHost: String,
    irohaPort: Int,
    private val credential: IrohaCredential
) : ChainListener<iroha.protocol.BlockOuterClass.Block> {
    val irohaApi = IrohaAPI(irohaHost, irohaPort)

    /**
     * Returns an observable that emits a new block every time it gets it from Iroha
     */
    override fun getBlockObservable(): Result<Observable<iroha.protocol.BlockOuterClass.Block>, Exception> {
        logger.info { "On subscribe to Iroha chain" }
        return ModelUtil.getBlockStreaming(irohaApi, credential).map { observable ->
            observable.map { response ->
                logger.info { "New Iroha block arrived. Height ${response.blockResponse.block.blockV1.payload.height}" }
                response.blockResponse.block
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
        irohaApi.close()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
