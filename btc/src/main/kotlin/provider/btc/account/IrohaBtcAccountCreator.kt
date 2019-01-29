package provider.btc.account

import com.github.kittinunf.result.Result
import provider.btc.address.AddressInfo
import registration.IrohaAccountCreator
import sidechain.iroha.consumer.IrohaConsumer

const val BTC_WHITE_LIST_KEY = "btc_whitelist"
const val BTC_CURRENCY_NAME_KEY = "bitcoin"

/*
    Class that is used to create Bitcoin accounts in Iroha
 */
class IrohaBtcAccountCreator(
    irohaConsumer: IrohaConsumer,
    notaryIrohaAccount: String
) {
    private val irohaAccountCreator = IrohaAccountCreator(irohaConsumer, notaryIrohaAccount, BTC_CURRENCY_NAME_KEY)

    /**
     * Creates new Bitcoin account to Iroha with given address
     * @param btcAddress - Bitcoin address
     * @param whitelist - list of addresses allowed to withdraw to
     * @param userName - client userName in Iroha
     * @param domain - client domain
     * @param pubkey - client's public key
     * @param notaryKeys - keys that were used to create given address
     * @return address associated with userName
     */
    fun create(
        btcAddress: String,
        whitelist: List<String>,
        userName: String,
        domain: String,
        pubkey: String,
        notaryKeys: List<String>
    ): Result<String, Exception> {
        return irohaAccountCreator.create(
            btcAddress,
            BTC_WHITE_LIST_KEY,
            whitelist,
            userName,
            domain,
            pubkey
        ) {
            AddressInfo(
                "$userName@$domain",
                notaryKeys
            ).toJson()
        }
    }
}
