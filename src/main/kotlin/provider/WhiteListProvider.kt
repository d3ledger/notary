package provider

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import model.IrohaCredential
import notary.endpoint.eth.EthRefundStrategyImpl
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.getAccountDetails

class WhiteListProvider(
    private val whiteListSetterAccount: String,
    private val credential: IrohaCredential,
    private val irohaNetwork: IrohaNetwork,
    private val whiteListKey: String
) {

    /**
     * Check if [srcAccountId] has withdrawal [address] in whitelist
     * @param srcAccountId - Iroha account - holder of whitelist
     * @param address - address to check
     * @return true if whitelist is not set, otherwise checks if [address] in the whitelist
     */
    fun checkWithdrawalAddress(srcAccountId: String, address: String): Result<Boolean, Exception> {
        return getAccountDetails(
            credential,
            irohaNetwork,
            srcAccountId,
            whiteListSetterAccount
        ).map { details ->
            val whitelist = details[whiteListKey]
            if (whitelist == null || whitelist.isEmpty()) {
                EthRefundStrategyImpl.logger.debug { "Whitelist is empty. Allow." }
                true
            } else {
                whitelist.split(", ").contains(address)
            }
        }
    }
}
