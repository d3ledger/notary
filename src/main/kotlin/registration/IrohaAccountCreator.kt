package registration

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import mu.KLogging
import notary.IrohaCommand
import notary.IrohaOrderedBatch
import notary.IrohaTransaction
import sidechain.iroha.CLIENT_DOMAIN
import sidechain.iroha.consumer.IrohaConsumer
import sidechain.iroha.consumer.IrohaConverterImpl
import sidechain.iroha.util.ModelUtil.getCurrentTime

class IrohaAccountCreator(
    private val irohaConsumer: IrohaConsumer,
    private val notaryIrohaAccount: String,
    private val currencyName: String
) {

    private val creator = irohaConsumer.creator

    /**
     * Creates new account to Iroha with given address
     * - CreateAccount with client name
     * - SetAccountDetail on client account with assigned [currencyAddress]
     * - SetAccountDetail on notary node account to mark [currencyAddress] as assigned to the particular user
     * @param currencyAddress - address of crypto currency wallet
     * @param whitelist - list of addresses allowed to withdraw to
     * @param userName - client userName in Iroha
     * @param pubkey - client's public key
     * @param notaryStorageStrategy - function that defines the way newly created account data will be stored in notary
     * @return address associated with userName
     */
    fun create(
        currencyAddress: String,
        whitelistKey: String,
        whitelist: List<String>,
        userName: String,
        pubkey: String,
        notaryStorageStrategy: () -> String
    ): Result<String, Exception> {
        return Result.of {

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
                                userName, CLIENT_DOMAIN, pubkey
                            ),
                            // Set user wallet/address in account detail
                            IrohaCommand.CommandSetAccountDetail(
                                "$userName@$CLIENT_DOMAIN",
                                currencyName,
                                currencyAddress
                            ),
                            // Set wallet/address as occupied by user id
                            IrohaCommand.CommandSetAccountDetail(
                                notaryIrohaAccount,
                                currencyAddress,
                                notaryStorageStrategy()
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
                                "$userName@$CLIENT_DOMAIN",
                                whitelistKey,
                                whitelist.toString().trim('[').trim(']')
                            )
                        )
                    )
                )
            )
        }.flatMap { irohaTx ->
            val utx = IrohaConverterImpl().convert(irohaTx)
            irohaConsumer.sendAndCheck(utx)
        }.map {
            logger.info { "New account $userName@$CLIENT_DOMAIN was created" }
            currencyAddress
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
