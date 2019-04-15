package com.d3.btc.withdrawal.handler

import com.d3.btc.provider.network.BtcNetworkConfigProvider
import iroha.protocol.Commands
import org.bitcoinj.core.Address
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Handles new change addresses
 */
@Component
class NewChangeAddressHandler(
    @Autowired private val transfersWallet: Wallet,
    @Autowired private val btcNetworkConfigProvider: BtcNetworkConfigProvider
) {

    /**
     * Handles change address creation event
     * @param createChangeAddressCommand - Iroha command with change address details
     */
    fun handleNewChangeAddress(createChangeAddressCommand: Commands.SetAccountDetail) {
        //Make new change address watched
        transfersWallet.addWatchedAddress(
            Address.fromBase58(
                btcNetworkConfigProvider.getConfig(),
                createChangeAddressCommand.key
            )
        )
    }
}
