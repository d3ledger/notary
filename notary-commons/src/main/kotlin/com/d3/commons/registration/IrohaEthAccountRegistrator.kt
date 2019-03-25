/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.registration

import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.github.kittinunf.result.Result

const val ETH_WHITE_LIST_KEY = "eth_whitelist"

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
     * @param whitelist - list of addresses allowed to withdraw to
     * @param userName - client userName in Iroha
     * @param domain - client domain
     * @param pubkey - client's public key
     * @return address associated with userName
     */
    fun register(
        walletAddress: String,
        whitelist: List<String>,
        userName: String,
        domain: String,
        pubkey: String
    ): Result<String, Exception> {
        return irohaAccountRegistrator.register(
            walletAddress,
            ETH_WHITE_LIST_KEY,
            whitelist,
            userName,
            domain,
            pubkey
        ) { "$userName@$domain" }
    }
}
