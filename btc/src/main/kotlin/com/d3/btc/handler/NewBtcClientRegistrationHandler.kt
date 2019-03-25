/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.btc.handler

import com.d3.btc.provider.network.BtcNetworkConfigProvider
import iroha.protocol.Commands
import mu.KLogging
import org.bitcoinj.core.Address
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.d3.commons.sidechain.iroha.CLIENT_DOMAIN

/**
 * Class that is used to handle client registration commands
 */
@Component
class NewBtcClientRegistrationHandler(
    @Autowired private val btcNetworkConfigProvider: BtcNetworkConfigProvider
) {
    /**
     * Handles newly registered Bitcoin addresses and adds addresses to current wallet object
     */
    fun handleNewClientCommand(command: Commands.Command, wallet: Wallet) {
        if (!isNewClientWasRegistered(command)) {
            return
        }
        val address = Address.fromBase58(
            btcNetworkConfigProvider.getConfig(),
            command.setAccountDetail.value
        )
        //Add new registered address to wallet
        if (wallet.addWatchedAddress(address)) {
            logger.info { "New BTC address ${command.setAccountDetail.value} was added to wallet" }
        } else {
            logger.error { "Address $address was not added to wallet" }
        }
    }

    // Checks if new btc client was registered
    private fun isNewClientWasRegistered(command: Commands.Command): Boolean {
        return command.setAccountDetail.accountId.endsWith("@$CLIENT_DOMAIN")
                && command.setAccountDetail.key == "bitcoin"
    }

    /**
     * Logger
     */
    companion object : KLogging()

}
