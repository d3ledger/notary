package com.d3.btc.provider.account

import com.d3.btc.model.AddressInfo
import com.github.kittinunf.result.Result
import com.d3.commons.registration.IrohaAccountCreator
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer

const val BTC_WHITE_LIST_KEY = "btc_whitelist"
const val BTC_CURRENCY_NAME_KEY = "bitcoin"

/*
    Class that is used to create Bitcoin accounts in Iroha
 */
class IrohaBtcAccountCreator(
    irohaConsumer: IrohaConsumer,
    notaryIrohaAccount: String
) {
    private val irohaAccountCreator =
        IrohaAccountCreator(irohaConsumer, notaryIrohaAccount, BTC_CURRENCY_NAME_KEY)

    /**
     * Creates new Bitcoin account to Iroha with given address
     * @param btcAddress - Bitcoin address
     * @param whitelist - list of addresses allowed to withdraw to
     * @param userName - client userName in Iroha
     * @param domain - client domain
     * @param pubkey - client's public key
     * @param notaryKeys - keys that were used to create given address
     * @param nodeId - node id
     * @return address associated with userName
     */
    fun create(
        btcAddress: String,
        whitelist: List<String>,
        userName: String,
        domain: String,
        pubkey: String,
        notaryKeys: List<String>,
        nodeId: String
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
                notaryKeys,
                nodeId
            ).toJson()
        }
    }
}
