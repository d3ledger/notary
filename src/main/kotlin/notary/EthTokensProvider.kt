package notary

import com.github.kittinunf.result.Result

/** Interface of an instance that provides with ethereum ERC20 token white list. */
interface EthTokensProvider {

    /** Returns token list in form of (Ethereum wallet -> token name) */
    fun getTokens(): Result<Map<String, String>, Exception>
}
