package com.d3.eth.provider

import com.github.kittinunf.result.Result

/** Interface of an instance that provides deployed ethereum relays */
interface EthRelayProvider {

    /** Returns relays in form of (ethereum wallet -> iroha user name) */
    fun getRelays(): Result<Map<String, String>, Exception>

    /** Get relays belonging to [irohaAccountId] */
    fun getRelaysByAccountId(irohaAccountId: String): Result<Set<String>, Exception>
}
