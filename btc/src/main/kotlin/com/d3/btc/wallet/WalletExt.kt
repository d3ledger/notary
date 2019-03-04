package com.d3.btc.wallet

import org.bitcoinj.wallet.Wallet
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileLock


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
