package notary

import com.github.kittinunf.result.Result

/** Interface of an instance that provides with ethereum wallet white list. */
interface EthWalletsProvider {

    /** Returns wallet list in form of (ethereum wallet -> iroha user name) */
    fun getWallets(): Result<Map<String, String>, Exception>
}
