package registration

import com.github.kittinunf.result.Result

/**
 * Strategy for registration of a new client
 */
interface RegistrationStrategy {

    /**
     * Register new client with whitelist
     */
    fun register(
        accountName: String,
        domainId: String,
        whitelist: List<String>,
        pubkey: String
    ): Result<String, Exception>

    /**
     * Get a number of free addresses for registration
     */
    fun getFreeAddressNumber(): Result<Int, Exception>
}
