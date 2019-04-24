package com.d3.btc.provider.account

import com.d3.btc.model.AddressInfo
import com.d3.commons.registration.IrohaAccountRegistrator
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.github.kittinunf.result.Result

const val BTC_CURRENCY_NAME_KEY = "bitcoin"

/*
    Class that is used to create Bitcoin accounts in Iroha
 */
class IrohaBtcAccountRegistrator(
    irohaConsumer: IrohaConsumer,
    notaryIrohaAccount: String
) {
    private val irohaAccountRegistrator =
        IrohaAccountRegistrator(irohaConsumer, notaryIrohaAccount, BTC_CURRENCY_NAME_KEY)

    /**
     * Creates new Bitcoin account to Iroha with given address
     * @param btcAddress - Bitcoin address
     * @param userName - client userName in Iroha
     * @param domain - client domain
     * @param pubkey - client's public key
     * @param notaryKeys - keys that were used to create given address
     * @param nodeId - node id
     * @return address associated with userName
     */
    fun create(
        btcAddress: String,
        userName: String,
        domain: String,
        pubkey: String,
        notaryKeys: List<String>,
        nodeId: String
    ): Result<String, Exception> {
        return irohaAccountRegistrator.register(
            btcAddress,
            userName,
            domain,
            pubkey
        ) {
            AddressInfo(
                "$userName@$domain",
                notaryKeys,
                nodeId,
                null
            ).toJson()
        }
    }
}
