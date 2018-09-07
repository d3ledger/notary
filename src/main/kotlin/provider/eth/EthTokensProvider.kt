package provider.eth

import com.github.kittinunf.result.Result

/** Information about token - token [name] and [precision] */
data class EthTokenInfo(val name: String, val precision: Int)

/** Interface of an instance that provides with ethereum ERC20 token white list. */
interface EthTokensProvider {

    /** Returns token list in form of (Ethereum wallet -> token name) */
    fun getTokens(): Result<Map<String, EthTokenInfo>, Exception>

    /** Adds new token to token list*/
    fun addToken(ethWallet: String, tokenInfo: EthTokenInfo): Result<Unit, Exception>
}
