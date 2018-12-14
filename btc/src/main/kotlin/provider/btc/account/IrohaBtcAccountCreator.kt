package provider.btc.account

import com.github.kittinunf.result.Result
import provider.btc.address.AddressInfo
import registration.IrohaAccountCreator
import sidechain.iroha.CLIENT_DOMAIN
import sidechain.iroha.consumer.IrohaConsumer

const val BTC_WHITE_LIST_KEY = "btc_whitelist"

/*
    Class that is used to create Bitcoin accounts in Iroha
 */
class IrohaBtcAccountCreator(
    irohaConsumer: IrohaConsumer,
    notaryIrohaAccount: String
) {
    private val irohaAccountCreator = IrohaAccountCreator(irohaConsumer, notaryIrohaAccount, "bitcoin")

    /**
     * Creates new Bitcoin account to Iroha with given address
     * @param btcAddress - Bitcoin address
     * @param whitelist - list of addresses allowed to withdraw to
     * @param userName - client userName in Iroha
     * @param pubkey - client's public key
     * @param notaryKeys - keys that were used to create given address
     * @return address associated with userName
     */
    fun create(
        btcAddress: String,
        whitelist: List<String>,
        userName: String,
        pubkey: String,
        notaryKeys: List<String>
    ): Result<String, Exception> {
        return irohaAccountCreator.create(
            btcAddress,
            BTC_WHITE_LIST_KEY,
            whitelist,
            userName,
            pubkey
        ) {
            AddressInfo(
                "$userName@$CLIENT_DOMAIN",
                notaryKeys
            ).toJson()
        }
    }
}
