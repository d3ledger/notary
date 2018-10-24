package registration

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.ModelTransactionBuilder
import jp.co.soramitsu.iroha.PublicKey
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
        val domain = CLIENT_DOMAIN
        return Result.of {
            ModelTransactionBuilder()
                .createdTime(getCurrentTime())
                .creatorAccountId(creator)
                .quorum(1)
                // Create account
                .createAccount(userName, domain, PublicKey(PublicKey.fromHexString(pubkey)))
                // Set user wallet/address in account detail
                .setAccountDetail("$userName@$domain", "${currencyName}_wallet", currencyAddress)
                // Set wallet/address as occupied by user id
                .setAccountDetail(notaryIrohaAccount, currencyAddress, "$userName@$domain")
                // Set whitelist
                .setAccountDetail("$userName@$domain", "${currencyName}_whitelist", whitelist)
                .build()
        }.map { utx ->
            irohaConsumer.sendAndCheck(utx)
        }.map {
            logger.info { "New account $userName@$domain was created" }
            currencyAddress
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
