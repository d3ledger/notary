package registration

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import mu.KLogging
import notary.IrohaCommand
import notary.IrohaOrderedBatch
import notary.IrohaTransaction
import sidechain.iroha.consumer.IrohaConsumer
import sidechain.iroha.consumer.IrohaConverterImpl
import sidechain.iroha.util.ModelUtil.getCurrentTime

class IrohaAccountCreator(
    private val irohaConsumer: IrohaConsumer,
    private val notaryIrohaAccount: String,
    private val addressName: String
) {

    private val creator = irohaConsumer.creator

    /**
     * Creates new account to Iroha with given address
     * - CreateAccount with client name
     * - SetAccountDetail on client account with assigned relay wallet from notary pool of free relay addresses
     * - SetAccountDetail on notary node account to mark relay address in pool as assigned to the particular user
     * @param currencyAddress - address of crypto currency wallet
     * @param whitelist - list of addresses allowed to withdraw to
     * @param userName - client userName in Iroha
     * @param pubkey - client's public key
     * @return address associated with userName
     */
    fun create(
        currencyAddress: String,
        whitelist: String,
        userName: String,
        pubkey: String
    ): Result<String, Exception> {
        return Result.of {
            val domain = "d3"
            // TODO: implement https://soramitsu.atlassian.net/browse/D3-415
            IrohaOrderedBatch(
                listOf(
                    IrohaTransaction(
                        creator,
                        getCurrentTime(),
                        1,
                        arrayListOf(
                            // Create account
                            IrohaCommand.CommandCreateAccount(
                                userName, domain, pubkey
                            ),
                            // Set user wallet/address in account detail
                            IrohaCommand.CommandSetAccountDetail(
                                "$userName@$domain",
                                addressName,
                                currencyAddress
                            ),
                            // Set wallet/address as occupied by user id
                            IrohaCommand.CommandSetAccountDetail(
                                notaryIrohaAccount,
                                currencyAddress,
                                "$userName@$domain"
                            )
                        )
                    ),
                    IrohaTransaction(
                        creator,
                        getCurrentTime(),
                        1,
                        arrayListOf(
                            //set whitelist
                            IrohaCommand.CommandSetAccountDetail(
                                "$userName@$domain",
                                "eth_whitelist",
                                whitelist
                            )
                        )
                    )
                )
            )
        }.flatMap { irohaTx ->
            val utx = IrohaConverterImpl().convert(irohaTx)
            irohaConsumer.sendAndCheck(utx)
        }.map {
            logger.info { "New account $userName was created" }
            currencyAddress
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
