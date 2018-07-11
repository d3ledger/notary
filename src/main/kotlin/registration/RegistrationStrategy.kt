package registration

import com.github.kittinunf.result.Result

/**
 * Strategy for registration of a new client
 */
interface RegistrationStrategy {

    /**
     * Register new client
     */
    fun register(name: String, pubkey: String): Result<String, Exception>
}
