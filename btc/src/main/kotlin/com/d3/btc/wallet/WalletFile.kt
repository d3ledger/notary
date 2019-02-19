package com.d3.btc.wallet

import mu.KLogging
import org.bitcoinj.wallet.Wallet
import java.io.File

/*
org.bitcoinj.wallet.Wallet object doesn't hold a file, which is used for saving wallet data into file system.
This is why wallet.WalletFile class was born. Now we can just call save(), instead of wallet.saveToFile(walletFile).
 */
class WalletFile(
    //BTC wallet
    val wallet: Wallet,
    //File to save wallet data
    private val file: File
) {
    fun save() {
        wallet.saveToFile(file)
        logger.info { "BTC wallet was saved to ${file.absolutePath}" }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
