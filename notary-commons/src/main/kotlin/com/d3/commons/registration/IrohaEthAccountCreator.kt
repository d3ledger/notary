package com.d3.commons.registration

import com.github.kittinunf.result.Result
import com.d3.commons.sidechain.iroha.CLIENT_DOMAIN
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer

const val ETH_WHITE_LIST_KEY = "eth_whitelist"

/*
    Class that is used to create Ethereum accounts in Iroha
 */
class IrohaEthAccountCreator(
    irohaConsumer: IrohaConsumer,
    notaryIrohaAccount: String
) {

    private val irohaAccountCreator =
        IrohaAccountCreator(irohaConsumer, notaryIrohaAccount, "ethereum_wallet")

    /**
     * Creates new Ethereum account to Iroha with given address
     * @param walletAddress - address of Ethereum wallet
     * @param whitelist - list of addresses allowed to withdraw to
     * @param userName - client userName in Iroha
     * @param domain - client domain
     * @param pubkey - client's public key
     * @return address associated with userName
     */
    fun create(
        walletAddress: String,
        whitelist: List<String>,
        userName: String,
        domain: String,
        pubkey: String
    ): Result<String, Exception> {
        return irohaAccountCreator.create(
            walletAddress,
            ETH_WHITE_LIST_KEY,
            whitelist,
            userName,
            domain,
            pubkey
        ) { "$userName@$CLIENT_DOMAIN" }
    }
}
