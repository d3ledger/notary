package registration

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.IrohaConfig
import notary.IrohaCommand
import notary.IrohaTransaction
import sidechain.iroha.consumer.IrohaConsumer
import sidechain.iroha.consumer.IrohaConverterImpl
import sidechain.iroha.consumer.IrohaNetworkImpl

/**
 * Effective implementation of [RegistrationStrategy]
 */
class RegistrationStrategyImpl(
    val ethFreeWalletsProvider: EthFreeWalletsProvider,
    val irohaConsumer: IrohaConsumer,
    val notaryIrohaAccount: String,
    val irohaConfig: IrohaConfig
) : RegistrationStrategy {

    /**
     * Register new notary client
     * - CreateAccount with client name
     * - SetAccountDetail on client account with assigned relay wallet from notary pool of free relay addresses
     * - SetAccountDetail on notary node account to mark relay address in pool as assigned to the particular user
     * @param name - client name
     * @param pubkey - client public key
     */
    override fun register(name: String, pubkey: String): Result<String, Exception> {
        lateinit var ethWallet: String

        return Result.of {
            ethWallet = ethFreeWalletsProvider.getWallet()
            val creator = irohaConfig.creator
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
        }.flatMap {
            val utx = IrohaConverterImpl().convert(it)
            val tx = irohaConsumer.convertToProto(utx)
            IrohaNetworkImpl(irohaConfig.hostname, irohaConfig.port).sendAndCheck(tx, utx.hash())
        }.map {
            ethWallet
        }
    }
}
