package registration

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import notary.IrohaCommand
import notary.IrohaTransaction
import provider.eth.EthFreeRelayProvider
import sidechain.iroha.consumer.IrohaConsumer
import sidechain.iroha.consumer.IrohaConverterImpl

/**
 * Effective implementation of [RegistrationStrategy]
 */
class RegistrationStrategyImpl(
    private val ethFreeRelayProvider: EthFreeRelayProvider,
    private val irohaConsumer: IrohaConsumer,
    private val notaryIrohaAccount: String,
    private val creator: String
) : RegistrationStrategy {

    /**
     * Register new notary client
     * - CreateAccount with client name
     * - SetAccountDetail on client account with assigned relay wallet from notary pool of free relay addresses
     * - SetAccountDetail on notary node account to mark relay address in pool as assigned to the particular user
     * @param name - client name
     * @param pubkey - client public key
     * @return ethereum wallet has been registered
     */
    override fun register(name: String, pubkey: String): Result<String, Exception> {
        return ethFreeRelayProvider.getRelay()
            .flatMap { freeEthWallet ->
                addIrohaEthAccount(freeEthWallet, name, pubkey)
            }
    }

    /**
     * Adds new account to Iroha with given Ethereum address
     * @param ethWallet - ethereum wallet
     * @param name - client name in Iroha
     * @param pubkey - client's public key
     * @return ethereum wallet
     */
    private fun addIrohaEthAccount(ethWallet: String, name: String, pubkey: String): Result<String, Exception> {
        return Result.of {
            val domain = "notary"
            IrohaTransaction(
                creator,
                arrayListOf(
                    // Create account
                    IrohaCommand.CommandCreateAccount(
                        name, domain, pubkey
                    ),
                    // Set user ethereum wallet in account detail
                    IrohaCommand.CommandSetAccountDetail(
                        "$name@$domain",
                        "ethereum_wallet",
                        ethWallet
                    ),
                    // Set ethereum wallet as occupied by user id
                    IrohaCommand.CommandSetAccountDetail(
                        notaryIrohaAccount,
                        ethWallet,
                        "$name@$domain"
                    )
                )
            )
        }.flatMap { irohaTx ->
            val utx = IrohaConverterImpl().convert(irohaTx)
            irohaConsumer.sendAndCheck(utx)
        }.map {
            ethWallet
        }
    }
}
