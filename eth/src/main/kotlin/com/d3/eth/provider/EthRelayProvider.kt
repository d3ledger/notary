package com.d3.eth.provider

import com.github.kittinunf.result.Result
import java.util.*

/** Interface of an instance that provides deployed ethereum relays */
interface EthRelayProvider {

    /** Returns relays in form of (ethereum wallet -> iroha user name) */
    fun getRelays(): Result<Map<String, String>, Exception>

    /**
     * Get relay belonging to [irohaAccountId]
     * @return relay or null if relay is absent
     */
    fun getRelayByAccountId(irohaAccountId: String): Result<Optional<String>, Exception>
}
