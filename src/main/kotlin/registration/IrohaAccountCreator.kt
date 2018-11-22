package registration

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.UnsignedTx
import mu.KLogging
import notary.IrohaCommand
import notary.IrohaOrderedBatch
import notary.IrohaTransaction
import sidechain.iroha.CLIENT_DOMAIN
import sidechain.iroha.consumer.IrohaConsumer
import sidechain.iroha.consumer.IrohaConverterImpl
import sidechain.iroha.util.ModelUtil.getCurrentTime

open class IrohaAccountCreator(
    private val irohaConsumer: IrohaConsumer,
    private val notaryIrohaAccount: String,
    private val currencyName: String
) {

    private val creator = irohaConsumer.creator

    /**
     * Creates new account to Iroha with given address
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
            createAccountCreationBatch(
                currencyAddress,
                whitelistKey,
                whitelist,
                userName,
                pubkey,
                notaryStorageStrategy
            )
        }.map { irohaTx ->
            val utx = convertBatchToTx(irohaTx)
            irohaConsumer.sendAndCheck(utx).fold({ passedTransactions ->
                if (isAccountCreationBatchFailed(utx, passedTransactions)) {
                    throw IllegalStateException("Cannot create account")
                }
            }, { ex -> throw ex })
        }.map {
            logger.info { "New account $userName@$CLIENT_DOMAIN was created" }
            currencyAddress
        }
    }

    /**
     * Converts batch into single Iroha transaction
     * @param batch - batch full of transactions
     * @return single Iroha transaction
     */
    protected open fun convertBatchToTx(batch: IrohaOrderedBatch) = IrohaConverterImpl().convert(batch)

    /**
     * Creates account creation batch with the following commands
     * - CreateAccount with client name
     * - SetAccountDetail on client account with assigned [currencyAddress]
     * - SetAccountDetail on notary node account to mark [currencyAddress] as assigned to the particular user
     * @param currencyAddress - address of crypto currency wallet
     * @param whitelist - list of addresses allowed to withdraw to
     * @param userName - client userName in Iroha
     * @param pubkey - client's public key
     * @param notaryStorageStrategy - function that defines the way newly created account data will be stored in notary
     * @return batch
     */
    protected open fun createAccountCreationBatch(
        currencyAddress: String,
        whitelistKey: String,
        whitelist: List<String>,
        userName: String,
        pubkey: String,
        notaryStorageStrategy: () -> String
    ): IrohaOrderedBatch {
        // TODO: implement https://soramitsu.atlassian.net/browse/D3-415
        return IrohaOrderedBatch(
            listOf(
                IrohaTransaction(
                    creator,
                    getCurrentTime(),
                    1,
                    arrayListOf(
                        // Create account
                        IrohaCommand.CommandCreateAccount(
                            userName, CLIENT_DOMAIN, pubkey
                        )
                    )
                ),
                IrohaTransaction(
                    creator,
                    getCurrentTime(),
                    1,
                    arrayListOf(
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
    }

    /**
     * Checks if account creation batch failed
     * @param unsignedTransactions - list of not processed transactions from batch
     * @param passedTransactions - list of successfully processed transactions
     * @return 'true' if all [unsignedTransactions] are in [passedTransactions] (except for first transaction, we can ignore it)
     */
    protected open fun isAccountCreationBatchFailed(
        unsignedTransactions: List<UnsignedTx>,
        passedTransactions: List<String>
    ): Boolean {
        var txCounter = 0
        unsignedTransactions.forEach { unsignedTransaction ->
            /*
             It's ok for the first transaction(Create Account) to be failed.
             It may happen when we want to provide new crypto currency services to already created account
            */
            if (txCounter != 0
                && !passedTransactions.any { passedTransaction -> passedTransaction == unsignedTransaction.hash().hex() }
            ) {
                return true
            }
            txCounter++
        }
        return false
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
