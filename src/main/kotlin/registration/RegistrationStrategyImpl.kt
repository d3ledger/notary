package registration

import com.github.kittinunf.result.Result

/**
 * Effective implementation of [RegistrationStrategy]
 */
class RegistrationStrategyImpl : RegistrationStrategy {

    /**
     * Register new notary client
     * - CreateAccount with client name
     * - SetAccountDetail on client account with assigned relay wallet from notary pool of free relay addresses
     * - SetAccountDetail on notary node account to mark relay address in pool as assigned to the particular user
     * @param name - client name
     * @param pubkey - client public key
     */
    override fun register(name: String, pubkey: String): Result<Unit, Exception> {
        // TODO register user in Iroha
        return Result.of { }
    }
}
