@file:JvmName("BtcRefreshWalletsMain")
package com.d3.btc.cli

import org.bitcoinj.params.RegTestParams
import org.bitcoinj.wallet.Wallet
import java.io.File

/**
 * Recreates Regtest wallets. Good fo testing.
 */
fun main(args: Array<String>) {
    val networkParams = RegTestParams.get()
    // Wallet for keys
    val keysWallet = Wallet(networkParams)
    // Wallet for transfers
    val transfersWallet = Wallet(networkParams)
    //Save files
    keysWallet.saveToFile(File("deploy/bitcoin/regtest/keys.d3.wallet"))
    transfersWallet.saveToFile(File("deploy/bitcoin/regtest/transfers.d3.wallet"))
}
