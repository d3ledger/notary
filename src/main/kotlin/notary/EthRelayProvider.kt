package notary

import com.github.kittinunf.result.Result

/** Interface of an instance that provides deployed ethereum relays */
interface EthRelayProvider {

    /** Returns relays in form of (ethereum wallet -> iroha user name) */
    fun getRelays(): Result<Map<String, String>, Exception>
}
