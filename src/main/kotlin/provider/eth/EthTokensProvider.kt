package provider.eth

import com.github.kittinunf.result.Result

/** Information about token - token [name] and [precision] */
data class EthTokenInfo(val name: String, val precision: Short)

/** Interface of an instance that provides with ethereum ERC20 token white list. */
interface EthTokensProvider {

    /** Returns token list in form of (Ethereum wallet -> token name) */
    fun getTokens(): Result<Map<String, EthTokenInfo>, Exception>

    /** Return token precision by asset name */
    fun getTokenPrecision(name: String): Result<Short, Exception>

    /** Return token precision by asset name */
    fun getTokenAddress(name: String): Result<String, Exception>

    /** Adds all given tokens in Iroha*/
    fun addTokens(tokens: Map<String, EthTokenInfo>): Result<Unit, Exception>
}
