package com.d3.btc.wallet

import com.d3.btc.model.BtcAddress
import com.github.kittinunf.result.Result
import mu.KLogging
import org.bitcoinj.core.Address
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.wallet.Wallet
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileLock

private val logger = KLogging().logger

/**
 * Saves wallet safely.
 * Need to use if wallet file is used among multiple JVM processes
 * @param walletPath - file path to wallet
 */
@Synchronized
fun Wallet.safeSave(walletPath: String) {
    lockFileApply(walletPath) { this.saveToFile(File(walletPath)) }
}

/**
 * Loads wallet file safely.
 * Need to use if wallet file is used among multiple JVM processes.
 * @param walletPath - file path to wallet
 * @return wallet from file
 */
@Synchronized
fun safeLoad(walletPath: String): Wallet {
    var wallet: Wallet? = null
    lockFileApply(walletPath) { wallet = Wallet.loadFromFile(File(walletPath)) }
    return wallet!!
}

/**
 * Checks wallet network
 * If wallet network differs from service network [IllegalStateException] will be thrown
 * @param serviceNetwork - network of service
 */
fun Wallet.checkWalletNetwork(
    serviceNetwork: NetworkParameters
): Result<Unit, Exception> {
    return Result.of {
        if (this.params.id != serviceNetwork.id) {
            throw IllegalStateException(
                "Bad wallet network parameters. " +
                        "Required ${serviceNetwork.id} but found ${this.params.id}"
            )
        }
    }
}

/**
 * Locks file and applies function inside lock
 * @param filePath - path of file to lock
 * @param apply - function that will be called inside lock
 */
private fun lockFileApply(filePath: String, apply: () -> Unit) {
    // Get a file channel for the file
    val file = File(filePath)
    var lock: FileLock? = null
    RandomAccessFile(file, "rw").channel.use { channel ->
        try {// Use the file channel to create a lock on the file.
            // This method blocks until it can retrieve the lock.
            lock = channel.lock()
            apply()
        } finally {
            // Release the lock - if it is not null!
            lock?.release()
        }
    }
}

/**
 * Adds given addresses to wallet's watched addresses
 * @param addresses - addresses to watch
 */
fun Wallet.addWatchedAddresses(addresses: List<BtcAddress>) {
    addresses.map { btcAddress ->
        Address.fromBase58(
            this.params,
            btcAddress.address
        )
    }.forEach { address ->
        if (this.addWatchedAddress(address)) {
            logger.info("Address $address was added to wallet")
        } else {
            logger.warn("Address $address was not added to wallet")
        }
    }
}

/**
 * Loads wallet using given file path and makes it "autosavable"
 * @param walletPath - path to a wallet
 * @return wallet
 */
fun loadAutoSaveWallet(walletPath: String): Wallet {
    val wallet = Wallet.loadFromFile(File(walletPath))
    // Save the wallet file on every received coin in order to track UTXO
    wallet.addCoinsReceivedEventListener { _, _, _, _ ->
        wallet.saveToFile(File(walletPath))
        logger.info("Got coin. Save wallet to $walletPath.")
    }
    // Save the wallet file on every sent coin in order to track UTXO
    wallet.addCoinsSentEventListener { _, _, _, _ ->
        wallet.saveToFile(File(walletPath))
        logger.info("Sent coin. Save wallet to $walletPath.")
    }
    return wallet
}
