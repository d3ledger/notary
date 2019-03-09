package com.d3.commons.sidechain.iroha

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import io.reactivex.Observable
import iroha.protocol.BlockOuterClass
import jp.co.soramitsu.iroha.java.IrohaAPI
import com.d3.commons.model.IrohaCredential
import mu.KLogging
import com.d3.commons.sidechain.ChainListener
import com.d3.commons.sidechain.iroha.util.ModelUtil

/**
 * Plain implementation of [ChainListener] with effective dependencies
 */
class IrohaChainListener(
    private val irohaAPI: IrohaAPI,
    private val credential: IrohaCredential
) : ChainListener<iroha.protocol.BlockOuterClass.Block> {
    constructor(
        irohaHost: String,
        irohaPort: Int,
        credential: IrohaCredential
    ) : this(IrohaAPI(irohaHost, irohaPort), credential)

    override fun getBlockObservable(): Result<Observable<BlockOuterClass.Block>, Exception> {
        logger.info { "On subscribe to Iroha chain" }
        return ModelUtil.getBlockStreaming(irohaAPI, credential).map { observable ->
            observable.map { response ->
                logger.info { "New Iroha block arrived. Height ${response.blockResponse.block.blockV1.payload.height}" }
                response.blockResponse.block
            }
        }
    }

    override suspend fun getBlock(): BlockOuterClass.Block {
        return getBlockObservable().get().blockingFirst()
    }

    override fun close() {
        irohaAPI.close()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
