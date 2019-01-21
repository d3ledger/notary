package provider

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.QueryAPI
import mu.KLogging
import sidechain.iroha.util.getAccountDetails

abstract class WhiteListProvider protected constructor(
    private val whiteListSetterAccount: String,
    private val queryAPI: QueryAPI,
    private val whiteListKey: String
) {
    /**
     * Check if [srcAccountId] has withdrawal [address] in whitelist.
     * @param srcAccountId - Iroha account - holder of whitelist
     * @param address - address to check
     * @return true if whitelist is not set or empty, otherwise checks if [address] in the whitelist
     */
    fun checkWithdrawalAddress(srcAccountId: String, address: String): Result<Boolean, Exception> {
        return getAccountDetails(
            queryAPI,
            srcAccountId,
            whiteListSetterAccount
        ).map { details ->
            val whitelist = details[whiteListKey]
            if (whitelist == null || whitelist.isEmpty()) {
                logger.debug { "Whitelist is empty. Allow." }
                true
            } else {
                whitelist.split(", ").contains(address)
            }
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
