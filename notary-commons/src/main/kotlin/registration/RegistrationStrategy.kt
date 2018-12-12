package registration

import com.github.kittinunf.result.Result
import java.security.PublicKey

/**
 * Strategy for registration of a new client
 */
interface RegistrationStrategy {

    /**
     * Register new client with whitelist
     */
    fun register(name: String, whitelist: List<String>, pubkey: PublicKey): Result<String, Exception>
}
