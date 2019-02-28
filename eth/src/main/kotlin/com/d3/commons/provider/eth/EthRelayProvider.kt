package com.d3.commons.provider.eth

import com.github.kittinunf.result.Result

/** Interface of an instance that provides deployed ethereum relays */
interface EthRelayProvider {

    /** Returns relays in form of (ethereum wallet -> iroha user name) */
    fun getRelays(): Result<Map<String, String>, Exception>

    /** Get relay belonging to [irohaAccountId] */
    fun getRelay(irohaAccountId: String): Result<String, Exception>
}
