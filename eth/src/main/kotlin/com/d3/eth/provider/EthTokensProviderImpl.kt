package com.d3.eth.provider

import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.fanout
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
 * @param irohaQueryHelper - iroha queries network layer
 * @param ethAnchoredTokenStorageAccount - tokenStorageAccount that contains details about Ethereum
 * anchored ERC20 tokens
 * @param ethAnchoredTokenSetterAccount - tokenSetterAccount that set details about ERC20 tokens
 * anchored in Ethereum
 * @param irohaAnchoredTokenStorageAccount - tokenStorageAccount that contains details about Iroha
 * anchored ERC20 tokens
 * @param irohaAnchoredTokenSetterAccount - tokenSetterAccount that set details about ERC20 tokens
 * anchored in Iroha
 */
class EthTokensProviderImpl(
    private val irohaQueryHelper: IrohaQueryHelper,
    private val ethAnchoredTokenStorageAccount: String,
    private val ethAnchoredTokenSetterAccount: String,
    private val irohaAnchoredTokenStorageAccount: String,
    private val irohaAnchoredTokenSetterAccount: String
) : EthTokensProvider {

    init {
        logger.info {
            """
                Init token provider
                Ethereum anchored token storage: '$ethAnchoredTokenStorageAccount', setter: '$ethAnchoredTokenSetterAccount'
                Iroha anchored token storage: '$irohaAnchoredTokenStorageAccount', setter: '$irohaAnchoredTokenSetterAccount'
            """.trimIndent()
        }
    }

    /**
     * Get tokens anchored in Ethereum.
     * @returns map (EthreumAddress -> TokenName)
     */
    override fun getEthAnchoredTokens(): Result<Map<String, String>, Exception> {
        return irohaQueryHelper.getAccountDetails(
            ethAnchoredTokenStorageAccount,
            ethAnchoredTokenSetterAccount
        )
    }

    /**
     * Get tokens anchored in Iroha.
     * @returns map (EthreumAddress -> TokenName)
     */
    override fun getIrohaAnchoredTokens(): Result<Map<String, String>, Exception> {
        return irohaQueryHelper.getAccountDetails(
            irohaAnchoredTokenStorageAccount,
            irohaAnchoredTokenSetterAccount
        )
    }

    /**
     * Get precision of [assetId] asset in Iroha.
     */
    override fun getTokenPrecision(assetId: String): Result<Int, Exception> {
        return if (assetId == "$ETH_NAME#$ETH_DOMAIN")
            Result.of { ETH_PRECISION }
        else irohaQueryHelper.getAssetPrecision(assetId)
    }

    /**
     * Get token address of [assetId] asset. For ether returns 0x0000000000000000000000000000000000000000
     */
    override fun getTokenAddress(assetId: String): Result<String, Exception> {
        return if (assetId == "$ETH_NAME#$ETH_DOMAIN")
            Result.of { ETH_ADDRESS }
        else irohaQueryHelper.getAccountDetails(
            ethAnchoredTokenStorageAccount,
            ethAnchoredTokenSetterAccount
        ).fanout {
            irohaQueryHelper.getAccountDetails(
                irohaAnchoredTokenStorageAccount,
                irohaAnchoredTokenSetterAccount
            )
        }.map { (ethAnchored, irohaAnchored) ->
            val res = ethAnchored.plus(irohaAnchored).filterValues { it == assetId }
            if (res.isEmpty())
                throw IllegalArgumentException("Token $assetId not found")
            res.keys.first()
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
