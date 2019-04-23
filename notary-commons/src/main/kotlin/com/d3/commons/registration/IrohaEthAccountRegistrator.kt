package com.d3.commons.registration

import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.github.kittinunf.result.Result

/*
    Class that is used to create Ethereum accounts in Iroha
 */
class IrohaEthAccountRegistrator(
    irohaConsumer: IrohaConsumer,
    notaryIrohaAccount: String
) {

    private val irohaAccountRegistrator =
        IrohaAccountRegistrator(irohaConsumer, notaryIrohaAccount, "ethereum_wallet")

    /**
     * Creates new Ethereum account to Iroha with given address
     * @param walletAddress - address of Ethereum wallet
     * @param userName - client userName in Iroha
     * @param domain - client domain
     * @param pubkey - client's public key
     * @return address associated with userName
     */
    fun register(
        walletAddress: String,
        userName: String,
        domain: String,
        pubkey: String
    ): Result<String, Exception> {
        return irohaAccountRegistrator.register(
            walletAddress,
            userName,
            domain,
            pubkey
        ) { "$userName@$domain" }
    }
}
