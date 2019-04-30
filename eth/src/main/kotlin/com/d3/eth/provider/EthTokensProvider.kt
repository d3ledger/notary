package com.d3.eth.provider

import com.github.kittinunf.result.Result

/** Interface of an instance that provides with ethereum ERC20 token white list. */
interface EthTokensProvider {

    /**
     * Returns ERC20 Ethereum anchored tokens list in form of
     * (Ethereum wallet -> iroha assetId)
     */
    fun getEthAnchoredTokens(): Result<Map<String, String>, Exception>

    /**
     * Returns ERC20 Iroha anchored tokens list in form of
     * (Ethereum wallet -> iroha assetId)
     */
    fun getIrohaAnchoredTokens(): Result<Map<String, String>, Exception>

    /** Return token precision by asset id */
    fun getTokenPrecision(assetId: String): Result<Int, Exception>

    /** Return token precision by asset id */
    fun getTokenAddress(assetId: String): Result<String, Exception>
}
