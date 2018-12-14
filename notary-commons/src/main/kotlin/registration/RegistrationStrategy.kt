package registration

import com.github.kittinunf.result.Result

/**
 * Strategy for registration of a new client
 */
interface RegistrationStrategy {

    /**
     * Register new client with whitelist
     */
    fun register(name: String, whitelist: List<String>, pubkey: String): Result<String, Exception>
}
