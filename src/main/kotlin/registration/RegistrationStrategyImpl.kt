package registration

import com.github.kittinunf.result.Result
import jp.co.soramitsu.iroha.Hash
import main.ConfigKeys
import notary.CONFIG
import notary.IrohaCommand
import notary.IrohaOrderedBatch
import notary.IrohaTransaction
import sidechain.iroha.consumer.IrohaConsumer
import sidechain.iroha.consumer.IrohaConverterImpl
import sidechain.iroha.consumer.IrohaNetworkImpl

/**
 * Effective implementation of [RegistrationStrategy]
 */
class RegistrationStrategyImpl(
    val ethFreeWalletsProvider: EthFreeWalletsProvider,
    val irohaConsumer: IrohaConsumer
) : RegistrationStrategy {

    /**
     * Register new notary client
     * - CreateAccount with client name
     * - SetAccountDetail on client account with assigned relay wallet from notary pool of free relay addresses
     * - SetAccountDetail on notary node account to mark relay address in pool as assigned to the particular user
     * @param name - client name
     * @param pubkey - client public key
     */
    override fun register(name: String, pubkey: String): Result<Unit, Exception> {
        return Result.of {
            val ethWallet = ethFreeWalletsProvider.getWallet()
            val creator = CONFIG[ConfigKeys.irohaCreator]
            val domain = "notary"
            val masterAccount = CONFIG[ConfigKeys.irohaMaster]

            val irohaOutput = IrohaOrderedBatch(
                arrayListOf(
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
                                masterAccount,
                                ethWallet,
                                "$name@$domain"
                            )
                        )
                    )
                )
            )

            lateinit var hash: Hash
            IrohaConverterImpl().convert(irohaOutput)
                .map {
                    hash = it.hash()
                    irohaConsumer.convertToProto(it)
                }
                .map { IrohaNetworkImpl().sendAndCheck(it, hash) }

            Unit
        }
    }
}
