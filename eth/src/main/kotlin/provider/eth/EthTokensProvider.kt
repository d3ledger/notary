package provider.eth

import com.github.kittinunf.result.Result

/** Interface of an instance that provides with ethereum ERC20 token white list. */
interface EthTokensProvider {

    /** Returns token list in form of (Ethereum wallet -> iroha assetId) */
    fun getTokens(): Result<Map<String, String>, Exception>

    /** Return token precision by asset id */
    fun getTokenPrecision(assetId: String): Result<Int, Exception>

    /** Return token precision by asset id */
    fun getTokenAddress(assetId: String): Result<String, Exception>
}
