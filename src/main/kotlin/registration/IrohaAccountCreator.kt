package registration

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import notary.IrohaCommand
import notary.IrohaTransaction
import sidechain.iroha.consumer.IrohaConsumer
import sidechain.iroha.consumer.IrohaConverterImpl
import sidechain.iroha.util.ModelUtil.getCurrentTime

class IrohaAccountCreator(
    private val irohaConsumer: IrohaConsumer,
    private val notaryIrohaAccount: String,
    private val creator: String,
    private val addressName: String
) {

    /**
     * Creates new account to Iroha with given address
     * - CreateAccount with client name
     * - SetAccountDetail on client account with assigned relay wallet from notary pool of free relay addresses
     * - SetAccountDetail on notary node account to mark relay address in pool as assigned to the particular user
     * @param address - address/wallet
     * @param userName - client userName in Iroha
     * @param pubkey - client's public key
     * @return address associated with userName
     */
    fun create(
        address: String,
        userName: String,
        pubkey: String
    ): Result<String, Exception> {
        return Result.of {
            val domain = "notary"
            IrohaTransaction(
                creator,
                getCurrentTime(),
                arrayListOf(
                    // Create account
                    IrohaCommand.CommandCreateAccount(
                        userName, domain, pubkey
                    ),
                    // Set user wallet/address in account detail
                    IrohaCommand.CommandSetAccountDetail(
                        "$userName@$domain",
                        addressName,
                        address
                    ),
                    // Set wallet/address as occupied by user id
                    IrohaCommand.CommandSetAccountDetail(
                        notaryIrohaAccount,
                        address,
                        "$userName@$domain"
                    )
                )
            )
        }.flatMap { irohaTx ->
            val utx = IrohaConverterImpl().convert(irohaTx)
            irohaConsumer.sendAndCheck(utx)
        }.map {
            address
        }
    }
}
