package com.d3.eth.provider

import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import mu.KLogging

const val ETH_NAME = "ether"
const val ETH_DOMAIN = "ethereum"
const val ETH_PRECISION: Int = 18
const val ETH_ADDRESS = "0x0000000000000000000000000000000000000000"

const val XOR_NAME = "xor"
const val SORA_DOMAIN = "sora"

/**
 * Implementation of [EthTokensProvider] with Iroha storage.
 *
 * @param queryHelper - iroha queries network layer
 * @param tokenStorageAccount - tokenStorageAccount that contains details
 * @param tokenSetterAccount - tokenSetterAccount that holds tokens in tokenStorageAccount account
 */
class EthTokensProviderImpl(
    private val queryHelper: IrohaQueryHelper,
    private val tokenStorageAccount: String,
    private val tokenSetterAccount: String
) : EthTokensProvider {

    init {
        logger.info { "Init token provider, storage: '$tokenStorageAccount', setter: '$tokenSetterAccount'" }
    }

    /**
     * Get all tokens. Returns EthreumAddress -> TokenName
     */
    override fun getTokens(): Result<Map<String, String>, Exception> {
        return queryHelper.getAccountDetails(
            tokenStorageAccount,
            tokenSetterAccount
        )
    }

    /**
     * Get precision of [assetId] asset in Iroha.
     */
    override fun getTokenPrecision(assetId: String): Result<Int, Exception> {
        return if (assetId == "$ETH_NAME#$ETH_DOMAIN")
            Result.of { ETH_PRECISION }
        else queryHelper.getAssetPrecision(assetId)
    }

    /**
     * Get token address of [assetId] asset. For ether returns 0x0000000000000000000000000000000000000000
     */
    override fun getTokenAddress(assetId: String): Result<String, Exception> {
        return if (assetId == "$ETH_NAME#$ETH_DOMAIN")
            Result.of { ETH_ADDRESS }
        else queryHelper.getAccountDetails(
            tokenStorageAccount,
            tokenSetterAccount
        ).map { tokens ->
            tokens.filterValues {
                it == assetId
            }.keys.first()
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
